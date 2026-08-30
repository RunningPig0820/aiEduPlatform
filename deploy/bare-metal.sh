#!/usr/bin/env bash
# AI Edu Platform 裸机一键部署 (demo 2-3 人, Ubuntu 24.04)
# 前提: 三个仓库为兄弟目录, 且本脚本在 aiEduPlatform/deploy/ 下
#   <父>/aiEduPlatform                      后端
#   <父>/aiEduPlatformModel/ai-edu-ai-service  Python
#   <父>/aiEduPlatformFront/ai-edu-front      前端
# 用法: sudo bash bare-metal.sh
set -euo pipefail

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

# ---------- 0. 前置检查 ----------
[[ $EUID -eq 0 ]] || { echo "请用 sudo 运行"; exit 1; }
for d in "$BACKEND_DIR" "$AI_DIR" "$FRONT_DIR"; do
  [[ -d "$d" ]] || { echo "缺少目录: $d (请确认三个仓库是兄弟目录)"; exit 1; }
done

# ---------- 1. 系统依赖 ----------
log "安装系统依赖 (Java 21 / Maven / Nginx / Python 3.13 / Node 20)..."
apt-get update -y
apt-get install -y software-properties-common curl
add-apt-repository -y ppa:deadsnakes/ppa >/dev/null 2>&1 || true
curl -fsSL https://deb.nodesource.com/setup_20.x | bash - || true
apt-get update -y
apt-get install -y openjdk-21-jdk maven nginx python3.13 python3.13-venv nodejs

# ---------- 2. 构建 Java 后端 ----------
log "构建 Java 后端 (mvn package)..."
( cd "$BACKEND_DIR/ai-edu-backend" \
  && mvn -q -pl ai-edu-interface -am clean package -Dmaven.test.skip=true )
mkdir -p "$JAR_DIR"
cp "$BACKEND_DIR"/ai-edu-backend/ai-edu-interface/target/*.jar "$JAR_DIR/app.jar"
log "jar → $JAR_DIR/app.jar"

# ---------- 3. Python venv + 依赖 ----------
log "创建 Python 3.13 venv 并安装依赖..."
python3.13 -m venv "$VENV_DIR"
"$VENV_DIR/bin/pip" install --no-cache-dir -r "$AI_DIR/requirements.txt" \
  -i https://mirrors.cloud.tencent.com/pypi/simple/ --timeout 120 \
  || "$VENV_DIR/bin/pip" install --no-cache-dir -r "$AI_DIR/requirements.txt"

# ---------- 4. 构建前端 ----------
log "构建前端 (npm ci + vite build)..."
( cd "$FRONT_DIR" && npm ci && npm run build )

# ---------- 5. 密钥 /etc/ai-edu.env ----------
if [[ ! -f "$ENV_FILE" ]]; then
  cp "$SCRIPT_DIR/.env.example" "$ENV_FILE"
  warn "已生成 $ENV_FILE, 请填写密钥后重新运行: sudo bash bare-metal.sh start"
else
  log "密钥文件已存在: $ENV_FILE"
fi

# ---------- 6. systemd 单元 ----------
log "写入 systemd 单元..."
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

# ---------- 7. Nginx ----------
log "写入 Nginx 配置..."
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
        proxy_pass http://127.0.0.1:9627;
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

# ---------- 8. 启动 ----------
systemctl daemon-reload
systemctl enable ai-edu-ai ai-edu-backend nginx
if grep -qE "^DOUBAO_API_KEY=.+" "$ENV_FILE"; then
  systemctl restart ai-edu-ai ai-edu-backend nginx
  log "✅ 已启动。浏览器访问 http://<服务器IP>/"
else
  warn "DOUBAO_API_KEY 未填, 填好后执行: sudo systemctl restart ai-edu-ai ai-edu-backend nginx"
fi

log "验证: systemctl status ai-edu-ai / ai-edu-backend / nginx"
log "日志: journalctl -u ai-edu-ai -f | journalctl -u ai-edu-backend -f"
