#!/usr/bin/env bash
# AI Edu Platform 裸机一键部署 (demo 2-3 人, Ubuntu 24.04)
# 前提: 三个仓库为兄弟目录, 且本脚本在 aiEduPlatform/deploy/ 下
#   <父>/aiEduPlatform                      后端
#   <父>/aiEduPlatformModel/ai-edu-ai-service  Python
#   <父>/aiEduPlatformFront/ai-edu-front      前端
#
# 用法:
#   sudo bash bare-metal.sh [all|backend|frontend]
#     all       (默认) 三服务同机(单台 2C4G)
#     backend   只部署 Java + Python(两台机时放 4G 机; Java 连 Python 走 127.0.0.1 必须同机)
#     frontend  只部署 Nginx + 前端(两台机时放 2G 机; /api 反代到 backend 机)
#   frontend 模式 /api 目标 = BACKEND_HOST(默认 127.0.0.1, 分机时填 backend 机 IP):
#     BACKEND_HOST=192.168.x.x sudo bash bare-metal.sh frontend
set -euo pipefail

MODE="${1:-all}"
BACKEND_HOST="${BACKEND_HOST:-127.0.0.1}"    # frontend 模式反代目标(分机时 = backend 机 IP)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PARENT="$(dirname "$(dirname "$SCRIPT_DIR")")"        # 三个仓库的父目录
BACKEND_DIR="$(dirname "$SCRIPT_DIR")"                # aiEduPlatform
AI_DIR="$PARENT/aiEduPlatformModel/ai-edu-ai-service"
FRONT_DIR="$PARENT/aiEduPlatformFront/ai-edu-front"

ENV_FILE="/etc/ai-edu.env"
JAR_DIR="/opt/ai-edu/backend"
VENV_DIR="/opt/ai-edu/venv"

log()  { echo -e "\033[1;32m[deploy]\033[0m $*"; }
warn() { echo -e "\033[1;33m[deploy!]\033[0m $*"; }

[[ $EUID -eq 0 ]] || { echo "请用 sudo 运行"; exit 1; }
case "$MODE" in all|backend|frontend) ;; *) echo "用法: $0 [all|backend|frontend]"; exit 1;; esac

# 校验当前机器需要的仓库
need_repo() { [[ -d "$1" ]] || { echo "缺少目录: $1"; exit 1; }; }
need_repo "$BACKEND_DIR"
if [[ "$MODE" != frontend ]]; then need_repo "$AI_DIR"; fi
if [[ "$MODE" != backend  ]]; then need_repo "$FRONT_DIR"; fi

# ---------- 1. 系统依赖(按角色) ----------
log "安装系统依赖..."
apt-get update -y
if [[ "$MODE" != frontend ]]; then
  apt-get install -y software-properties-common
  add-apt-repository -y ppa:deadsnakes/ppa >/dev/null 2>&1 || true
  apt-get install -y openjdk-21-jdk maven python3.13 python3.13-venv
fi
if [[ "$MODE" != backend ]]; then
  apt-get install -y curl nginx
  curl -fsSL https://deb.nodesource.com/setup_20.x | bash - || true
  apt-get install -y nodejs
fi

# ---------- 2. Java 后端(backend / all) ----------
if [[ "$MODE" != frontend ]]; then
  log "构建 Java 后端 (mvn package)..."
  ( cd "$BACKEND_DIR/ai-edu-backend" \
    && mvn -q -pl ai-edu-interface -am clean package -Dmaven.test.skip=true )
  mkdir -p "$JAR_DIR"
  cp "$BACKEND_DIR"/ai-edu-backend/ai-edu-interface/target/*.jar "$JAR_DIR/app.jar"
  log "jar → $JAR_DIR/app.jar"
fi

# ---------- 3. Python venv(backend / all) ----------
if [[ "$MODE" != frontend ]]; then
  log "创建 Python 3.13 venv 并安装依赖..."
  python3.13 -m venv "$VENV_DIR"
  "$VENV_DIR/bin/pip" install --no-cache-dir -r "$AI_DIR/requirements.txt" \
    -i https://mirrors.cloud.tencent.com/pypi/simple/ --timeout 120 \
    || "$VENV_DIR/bin/pip" install --no-cache-dir -r "$AI_DIR/requirements.txt"
fi

# ---------- 4. 前端构建(frontend / all) ----------
if [[ "$MODE" != backend ]]; then
  log "构建前端 (npm ci + vite build)..."
  ( cd "$FRONT_DIR" && npm ci && npm run build )
fi

# ---------- 5. 密钥 /etc/ai-edu.env(backend / all) ----------
if [[ "$MODE" != frontend ]]; then
  if [[ ! -f "$ENV_FILE" ]]; then
    cp "$SCRIPT_DIR/.env.example" "$ENV_FILE"
    warn "已生成 $ENV_FILE, 请填写密钥后重新运行: sudo bash bare-metal.sh backend"
  else
    log "密钥文件已存在: $ENV_FILE"
  fi
fi

# ---------- 6. systemd 单元(按角色) ----------
log "写入 systemd 单元..."
if [[ "$MODE" != frontend ]]; then
cat > /etc/systemd/system/ai-edu-ai.service <<EOF
[Unit]
Description=AI Edu Python Service (FastAPI)
After=network.target

[Service]
WorkingDirectory=$AI_DIR
EnvironmentFile=$ENV_FILE
ExecStart=$VENV_DIR/bin/uvicorn main:app --host 0.0.0.0 --port 9527
Restart=always
RestartSec=3

[Install]
WantedBy=multi-user.target
EOF

cat > /etc/systemd/system/ai-edu-backend.service <<EOF
[Unit]
Description=AI Edu Java Backend
After=network.target ai-edu-ai.service

[Service]
WorkingDirectory=$BACKEND_DIR/ai-edu-backend
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar $JAR_DIR/app.jar
Restart=always
RestartSec=3

[Install]
WantedBy=multi-user.target
EOF
fi

# ---------- 7. Nginx(frontend / all) ----------
if [[ "$MODE" != backend ]]; then
  log "写入 Nginx 配置(反代 /api → $BACKEND_HOST:9627)..."
  cat > /etc/nginx/conf.d/ai-edu.conf <<EOF
server {
    listen 80;
    server_name _;
    root $FRONT_DIR/dist;
    index index.html;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://$BACKEND_HOST:9627;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 300s;
    }
}
EOF
  rm -f /etc/nginx/sites-enabled/default
  nginx -t
fi

# ---------- 8. 启动 ----------
systemctl daemon-reload
if [[ "$MODE" == frontend ]]; then
  systemctl enable nginx && systemctl restart nginx
  log "✅ 前端已启动。浏览器访问 http://<本机IP>/  (后端在 $BACKEND_HOST:9627)"
elif [[ "$MODE" == backend ]]; then
  systemctl enable ai-edu-ai ai-edu-backend
  if grep -qE "^DOUBAO_API_KEY=.+" "$ENV_FILE"; then
    systemctl restart ai-edu-ai ai-edu-backend
    log "✅ 后端已启动(本机 9527/9627)。"
  else
    warn "DOUBAO_API_KEY 未填, 填好后: sudo systemctl restart ai-edu-ai ai-edu-backend"
  fi
else
  systemctl enable ai-edu-ai ai-edu-backend nginx
  if grep -qE "^DOUBAO_API_KEY=.+" "$ENV_FILE"; then
    systemctl restart ai-edu-ai ai-edu-backend nginx
    log "✅ 三服务已启动。浏览器访问 http://<服务器IP>/"
  else
    warn "DOUBAO_API_KEY 未填, 填好后: sudo systemctl restart ai-edu-ai ai-edu-backend nginx"
  fi
fi

log "验证: systemctl status ai-edu-ai / ai-edu-backend / nginx"
log "日志: journalctl -u ai-edu-ai -f | journalctl -u ai-edu-backend -f"
