# kg-knowledge-overview 能力规格

按学段分页列全量教材知识点（带章节/小节归属），供学生端"知识点总览"知识地图底图。

## ADDED Requirements

### Requirement: 按学段分页列全量知识点

系统 SHALL 提供 `POST /api/kg/knowledge-points`（body `{stage, page, size}`），按学段分页返回教材知识点，每项含 `kpUri`/`kpLabel`/`stage`/`chapterLabel`/`sectionLabel`，并返回 `total`/`page`/`size`。数据源为 kg 镜像的教材知识点，按教材→章节→小节→知识点层级归属反查。

#### Scenario: 按学段分页返回知识点
- **WHEN** 调用 `POST /api/kg/knowledge-points` 携带 `{stage: "middle", page: 1, size: 20}`
- **THEN** 返回初中教材下的知识点条目（含 chapterLabel/sectionLabel），total 为初中知识点总数

#### Scenario: 无知识点返回空
- **WHEN** 某学段下无知识点（教材未同步）
- **THEN** 返回 items=[]，total=0

### Requirement: 分页边界

分页参数 SHALL 有默认值（page=1，size=20），size 有上限（防止一次拉全量）；page 越界 SHALL 返回空列表而非报错。

#### Scenario: 参数缺省与越界
- **WHEN** 未传 page/size，或 page 超出总页数
- **THEN** 使用默认值 / 返回空列表，不报错

### Requirement: 权威图谱零写入

知识点总览查询 SHALL 仅读 kg 镜像，SHALL NOT 向 Neo4j 或 kg-sync 镜像写入任何节点/边/行。

#### Scenario: 只读查询
- **WHEN** 分页查询知识点总览
- **THEN** 无任何 kg 镜像 / Neo4j 写操作
