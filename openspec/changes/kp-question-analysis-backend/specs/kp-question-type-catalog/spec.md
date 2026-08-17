# kp-question-type-catalog 能力规格（delta）

聚合新增变体别名合并：相似题型名收敛到 canonical，聚合阈值不再被变体拆分稀释。既有聚合/审核/LLM 关联行为不变。

## ADDED Requirements

### Requirement: 变体题型别名合并

离线聚合 SHALL 在建新 CANDIDATE 前比对现有 CANDIDATE/STABLE 题型的 kp_uri 分布重叠；重叠 ≥ 阈值（默认 70%）SHALL 视同变体：将本桶观测折叠进该 canonical 条目（更新 kp 分布桶统计与 hit 统计）并记录变体名为别名，SHALL NOT 新建重复 CANDIDATE。无相似 SHALL 才新建 CANDIDATE。

#### Scenario: 变体折叠进 canonical
- **WHEN** 「鸡兔同笼」（2 学生）与「鸡兔同笼问题」（2 学生）指向同一批 kp_uri，kp 分布重叠 ≥70%
- **THEN** 合并为单一 canonical 条目，别名「鸡兔同笼」关联该条目，聚合统计合并（4 学生）
- **AND** 聚合阈值按合并后统计判定，不再因变体拆分而各自不达标

#### Scenario: 无相似则新建
- **WHEN** 新题型名的 kp_uri 集合与现有题型重叠 <70%
- **THEN** 新建独立 CANDIDATE 条目

#### Scenario: 别名命中聚合统计
- **WHEN** 后续「鸡兔同笼」新观测进入聚合
- **THEN** 按别名命中 canonical 条目更新统计，不新建条目
