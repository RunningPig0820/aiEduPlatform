# kp-mastery-lightup 能力规格

掌握度在知识图谱上点亮：掌握度数据增强、学生端图谱页、挂起审核管理面。

## ADDED Requirements

### Requirement: 掌握度数据携带状态与置信度

`GET /api/students/{id}/mastery` 返回的每项 SHALL 包含 `kpKey`(URI)、`kpLabel`、`masteryLevel`、`status`(RESOLVED/PENDING)、`confidence`、`updatedAt`。status/confidence 供前端区分"确定掌握"与"疑似待确认"。

#### Scenario: 掌握度返回增强字段
- **WHEN** 学生掌握度查询成功
- **THEN** 每项含 status 与 confidence，图谱可据此渲染档位

### Requirement: 学生端图谱按掌握度点亮

学生端知识图谱页 SHALL 按 `node.id`(URI) 与 `mastery.kpKey` 匹配，对节点着色：mastered(≥75) 绿 / practicing(50) 黄 / struggling(25) 红；无掌握度数据节点保持中性灰。学生端图谱页 SHALL 复用现有图谱组件，SHALL NOT 影响 admin 图谱页。

#### Scenario: 掌握节点点亮
- **WHEN** 学生在七年级图谱查看，且其「二元一次方程组」掌握度 75
- **THEN** 该节点以绿色"掌握"档位渲染

#### Scenario: 学生端与管理员端隔离
- **WHEN** 学生访问知识图谱路由
- **THEN** 仅看到点亮视图，不暴露同步管理/系统统计等管理功能

### Requirement: 疑似薄弱节点可见不点亮

对解析 PENDING 或低置信的题型，图谱 SHALL 以"疑似待确认"视觉渲染（虚线 + 待确认角标），SHALL NOT 将其写入掌握度或按确认薄弱（红）着色。

#### Scenario: 疑似节点渲染
- **WHEN** 学生答疑产生「鸡兔同笼」PENDING 观测且该节点存在于当前图谱
- **THEN** 节点以虚线 + 待确认角标渲染，区别于确认薄弱（红）

### Requirement: 挂起审核管理面

系统 SHALL 提供管理接口：`GET /api/kg/aliases/pending` 列出挂起观测（HUMAN_REVIEW/PENDING），`POST /api/kg/aliases/pending/{id}/confirm` 确认其 kp_uri。确认后 SHALL 更新观测状态并回流题型库统计。仅 ADMIN/TEACHER 可访问。

#### Scenario: 人工确认挂起题型
- **WHEN** 管理员将挂起的「鸡兔同笼」确认归属假设法 URI
- **THEN** 观测转 RESOLVED，题型库假设法分布桶命中数增加，后续解析命中假设法
