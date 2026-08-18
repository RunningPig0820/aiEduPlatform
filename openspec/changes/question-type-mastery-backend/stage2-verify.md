# 阶段 2 完成标准验证脚本（tasks 2.8.3）

> 目标：造题目数据 → 动态聚集（经 Python 向量桥）→ 掌握表，不依赖 AI 答疑。
> 单测链已覆盖本链路全部逻辑（817 tests 全绿）；本脚本是 **真实 DB 端到端** 的执行步骤，
> 需 MySQL + Java 应用 + Python 服务就绪后运行。

## 前置

| 依赖 | 就绪方式 |
|------|---------|
| MySQL | `cd deploy && docker-compose up -d`（含 MySQL 3306） |
| 迁移 | Flyway 关闭——执行 `docs/db/05_learning_domain.sql` 全部 DDL/DML（含 V17/V18/V19） |
| Java 应用 | `cd ai-edu-backend/ai-edu-interface && mvn spring-boot:run`（端口 8080） |
| Python | `localhost:9527`（向量端点已交付 b7159c5，`/health` → healthy） |
| 登录态 | 任选：直连接口绕过 session，或用学生/ADMIN session 调接口 |

## 步骤

### ① 造题目数据（带 score 模拟答疑落库；canonical_label 留空 = PENDING 待归并）

```sql
INSERT INTO t_student_question_record
  (student_id, content, source, topic_label, canonical_label, score, hint_count, answer_request_count, session_id, created_at, updated_at, created_by, modified_by, is_deleted)
VALUES
  (1001, '笼子里有鸡和兔共 35 个头 94 只脚', 'ai', '鸡兔同笼问题', NULL, 1.00, 0, 0, 1, NOW(), NOW(), 0, 0, 0),
  (1001, '解一元二次方程 x²-5x+6=0',           'ai', '解一元二次方程', NULL, 0.50, 1, 0, 1, NOW(), NOW(), 0, 0, 0),
  (1001, '一元二次方程 x²+2x+1=0',             'ai', '一元二次方成',   NULL, 0.00, 0, 0, 1, NOW(), NOW(), 0, 0, 0);
```

### ② 触发批量聚集（ADMIN，经 Python 向量桥）

```bash
# 首题建锚后 put 向量 ~10s 异步生效——topic-cluster 里「先归并后建锚」顺序天然容忍；
# 若同批散名依赖「已建锚向量」，可先跑一次建锚，等 ≥10s 再跑一次补归并（幂等）
curl -X POST http://localhost:8080/api/kp/aggregation/topic-cluster
  -H "Cookie: SESSION=<admin-session>"
# → { pendingTopics: 3, mergedTopics: 2 }（鸡兔同笼问题→鸡兔同笼、一元二次方成→一元二次方程；
#   解一元二次方程 经字符规则归「一元二次方程」后池命中；3 散名全部归并，不裂行）
```

### ③ 查掌握表（验证归并 + 累计平均）

```bash
curl http://localhost:8080/students/1001/mastery -H "Cookie: SESSION=<student-session>"
```

期望：
| topicLabel | masteryLevel | trainCount | source | 说明 |
|-----------|-------------|-----------|--------|------|
| 鸡兔同笼 | 100 | 1 | ai | 「鸡兔同笼问题」归并，score 1.00 |
| 一元二次方程 | 25 | 2 | ai | 「解一元二次方程」(0.5) +「一元二次方成」(0.0) 归并，(50+0)/2=25% |

**通过标准**：3 个散名全部归到 2 个 canonical（不裂行）、掌握表=累计平均正确率、全程未依赖 AI 答疑。
