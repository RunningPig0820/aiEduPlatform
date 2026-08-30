# AI Edu Platform 一键部署(demo, 2-3 人)

三服务容器化: **Java 后端(9627) + Python AI 服务(9527) + Nginx 前端(80)**。
云资源(腾讯云 MySQL/Redis/Neo4j/COS)已硬编码在 `ai-edu-backend/.../application.yml`,无需本地基础设施。

## 服务器需求

- **2C4G** Linux(Ubuntu 24.04 / Docker CE 即可),20GB 盘。
- 安装 Docker + docker compose(用腾讯云镜像"**Docker CE 27.5.1**"自带,无需另装)。

## 目录结构要求(三个仓库为兄弟目录)

```
<父目录>/
├── aiEduPlatform/                  # 后端(本仓库)
│   └── deploy/                     # ← 在此执行部署
├── aiEduPlatformModel/ai-edu-ai-service/   # Python AI 服务
└── aiEduPlatformFront/ai-edu-front/       # 前端
```

服务器上把这三个仓库 clone/上传成上述布局即可。

## 部署步骤

```bash
# 1. 进入 deploy
cd aiEduPlatform/deploy

# 2. 填密钥
cp .env.example .env
vim .env      # 必填 4 个: DOUBAO_API_KEY / DASHSCOPE_API_KEY / COS_VECTORS_SECRET_ID / COS_VECTORS_SECRET_KEY

# 3. 一键起(首次构建 3~5 分钟, 含 maven/npm/pip 下载)
docker compose up -d --build

# 4. 验证
docker compose ps                 # 三容器 running
curl -s http://127.0.0.1:80/       # 前端首页
curl -s http://127.0.0.1:9527/api/rag/assistant/guide?current_project=ai-tutoring -H "x-internal-token: my-secret-token-123"   # Python 冒烟
curl -s http://127.0.0.1:9627/api/auth/...   # Java 冒烟(或直接开浏览器)
```

浏览器访问 `http://<服务器IP>/` → 演示学生登录(admin/student 演示账号) → 逐功能验收。

## 环境变量说明

| 变量 | 必填 | 说明 |
|---|---|---|
| `DOUBAO_API_KEY` | ✅ | 火山方舟 doubao,答疑+RAG 生成 |
| `DASHSCOPE_API_KEY` | ✅ | 阿里 embedding,向量索引 |
| `COS_VECTORS_SECRET_ID/KEY` | ✅ | 腾讯云向量桶 + RAG 原文 |
| `INTERNAL_TOKEN` | ✅ | 与 Java `application.yml` 一致(默认 my-secret-token-123) |

Java 侧密钥(MySQL/Redis/Neo4j/COS)已硬编码在 `application.yml`,demo 可直接用;上线前建议改环境变量覆盖(`APP_ENCRYPT_AES_KEY` 等)。

## 常见问题

- **登录/接口偶发 500**:腾讯云 Redis 走公网链路不稳(日志 `Operation timed out`)。可加 `spring.data.redis.timeout: 3s` 快速失败重试;根治走腾讯云 VPC 私网。
- **SSE 流式不出字**:确认 nginx 已关缓冲(本配置已 `proxy_buffering off`)。
- **重建**:改代码后 `docker compose up -d --build`(增量,只重建变更服务)。
- **日志**:`docker compose logs -f backend|ai-service|frontend`。
- **停**:`docker compose down`。

## 端口

| 服务 | 端口 |
|---|---|
| 前端(浏览器入口) | 80 |
| Java 后端 | 9627 |
| Python AI 服务 | 9527 |
