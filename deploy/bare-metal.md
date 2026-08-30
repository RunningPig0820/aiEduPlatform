# AI Edu Platform 裸机部署(推荐 demo 用,Ubuntu 24.04)

> 不引入 Docker,三服务直接跑:**Java(jar)+ Python(venv)+ Nginx(前端静态 + 反代)**。
> 面向 2-3 人 demo;以后要容器化再统一用 `docker-compose.yml`(备用方案,三个仓库的 Dockerfile 已就绪)。

## 服务器需求

- **单台**:2C4G 足够(Java+Python+Nginx 全装)。
- **两台(推荐,更宽裕)**:
  - **2C4G** → Java 后端 + Python AI 服务(**必须同机**:Java 连 Python 走 `127.0.0.1:9527`)
  - **2C2G** → Nginx + 前端静态(很轻)
- Ubuntu 24.04 纯净版,20GB 盘。

## 目录结构(三个仓库兄弟目录)

```
<父目录>/
├── aiEduPlatform/                    # 后端(含本 deploy/)
├── aiEduPlatformModel/ai-edu-ai-service/
└── aiEduPlatformFront/ai-edu-front/
```

> 两台机各自上传/克隆**对应需要的仓库**(backend 机只要后端+Python;frontend 机只要前端)。

## 部署步骤

### 单台(三服务同机)

```bash
# 1. 三个仓库上传成兄弟目录
# 2. 一键部署(装依赖 + 构建 jar/venv/前端 + 配 systemd/nginx)
sudo bash aiEduPlatform/deploy/bare-metal.sh          # 默认 all
# 3. 填密钥 + 启动
sudo vim /etc/ai-edu.env
sudo systemctl restart ai-edu-ai ai-edu-backend nginx
```

### 两台(分机)

```bash
# 后端机(2C4G): 只装 Java + Python
sudo bash aiEduPlatform/deploy/bare-metal.sh backend
sudo vim /etc/ai-edu.env    # 必填密钥
sudo systemctl restart ai-edu-ai ai-edu-backend

# 前端机(2C2G): 只装 Nginx + 前端, /api 反代到后端机 IP
BACKEND_HOST=<后端机IP> sudo bash aiEduPlatform/deploy/bare-metal.sh frontend
```

浏览器访问 `http://<前端机IP>/` → 演示账号登录 → 逐功能验收。

## 三个服务

| 服务 | 端口 | 常驻 |
|---|---|---|
| 前端(Nginx) | 80 | systemd `nginx` |
| Java 后端 | 9627 | systemd `ai-edu-backend` |
| Python AI 服务 | 9527 | systemd `ai-edu-ai` |

- 启动:`systemctl start ai-edu-ai ai-edu-backend nginx`(已 `enable`,开机自启)
- 日志:`journalctl -u ai-edu-backend -f` / `journalctl -u ai-edu-ai -f`
- 重建(改代码后):`sudo bash bare-metal.sh` 重跑即可(幂等)

## 密钥清单(`/etc/ai-edu.env`)

| 变量 | 必填 | 说明 |
|---|---|---|
| `DOUBAO_API_KEY` | ✅ | 火山方舟 doubao(答疑+RAG 生成) |
| `DASHSCOPE_API_KEY` | ✅ | 阿里 embedding(向量索引) |
| `COS_VECTORS_SECRET_ID/KEY` | ✅ | 腾讯云向量桶 + RAG 原文 |
| `COS_VECTORS_RAG_BUCKET` | ✅ | 默认 `rag-1318177119` |
| `INTERNAL_TOKEN` | ✅ | 与 Java `application.yml` 一致 |

> Java 侧密钥(MySQL/Redis/Neo4j/COS)已硬编码在 `ai-edu-backend/ai-edu-interface/src/main/resources/application.yml`,demo 直接用。

## 常见问题

- **登录/接口偶发 500**:腾讯云 Redis 公网链路不稳(日志 `Operation timed out`)。可加 `spring.data.redis.timeout: 3s` 快速失败重试;根治走腾讯云 VPC 私网。
- **SSE 流式不出字**:确认 nginx 已关缓冲(本配置已 `proxy_buffering off`)。
- **Python 3.13**:脚本走 deadsnakes PPA 安装;若你的 Ubuntu 版本不同,确认 `python3.13` 可用。
- **前端 build 需要 Node 20**:脚本经 NodeSource 装 Node 20(Vite 5 要求 18+)。

## 与 Docker 方案的关系

裸机 = 当前推荐。Docker(`deploy/docker-compose.yml` + 各仓库 Dockerfile)= 备用,以后要容器化三服务统一走 compose,不单独容器化某一个。
