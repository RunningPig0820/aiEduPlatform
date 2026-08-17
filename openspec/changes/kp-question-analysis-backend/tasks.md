# kp-question-analysis-backend 实施任务

## 1. 数据层（V16 迁移 + 领域模型 + 仓储）

- [x] 1.1 新增 Flyway 迁移（learning 库）：`t_kp_question_type_alias`（id 自增、alias_label VARCHAR UNIQUE、question_type_id BIGINT FK、created_at），含 UNIQUE 索引与 FK；SQL 记录到 `docs/db/05_learning_domain.sql`；标注「Flyway 关闭，需手动执行」
- [x] 1.2 learning 领域新增 `QuestionTypeAlias` 实体（aliasLabel/questionTypeId/createdAt，JPA 注解、Lombok @Getter、restore/create 工厂） + `QuestionTypeAliasRepository` 接口（`upsert`、`findByAliasLabel`）
- [x] 1.3 infra 层别名 MyBatis-Plus 实现（`QuestionTypeAliasPo`/`QuestionTypeAliasMapper`/`QuestionTypeAliasRepositoryImpl`，`@DS("learning")`）

## 2. 题目理解（题目文本 → 题型名）

- [x] 2.1 learning 领域新增 `QuestionUnderstandingPort` 端口：`List<String> understand(String questionText, Integer grade)`（纯识别，不查库不落库）
- [x] 2.2 infra 新增 `KpQuestionAnalyzer`（`@Component` 实现端口）：LLM prompt「识别数学题题型名，每行一个，限 1~5 个，不编号不解释」+ **注入题型库 top-20 常用题型名作参考词表**（`findTopTopicLabels`）；解析复用 `KpLlmDisambiguator.parseNames` 去编号/bullet 逻辑；LLM 失败返回空列表
- [x] 2.3 `QuestionTypeRepository` 增加 `findTopTopicLabels(int limit)`（按 hitCount 降序取题型名）

## 3. analyze-question 端点

- [x] 3.1 `TutoringKpResolverImpl` 抽 `persistObs` 开关：`resolve(label, studentId)` 保持写 obs，暴露只读解析（`persistObs=false` 不写 `t_kp_derived_obs`）供 analyze 复用
- [x] 3.2 `QuestionTypeRepository` 增加 `findByTopicLabelOrAlias(String)`（canonical → 别名兜底，单 SQL 或两次查询）
- [x] 3.3 application 新增 `KpQuestionAnalysisAppService.analyze(text, studentId)`：① `understand` 候选题型名（空→PENDING 无候选）→ ② 首个候选 `findByTopicLabelOrAlias` 命中 → RESOLVED 全分布（kpLabel 反查，同题型库关联接口）→ ③ 未命中 → 只读 `resolve` 单点（RESOLVED ratio=1 / PENDING candidates）→ ④ 全候选 PENDING → 返回首个候选 PENDING 结果
- [x] 3.4 interface `KpResolutionController` 加 `POST /api/kp/analyze-question`（`{ text }` → `ApiResponse<QuestionAnalysisDTO>`：topicLabel/status/confidence/knowledgePoints[{kpUri,kpLabel,gradeRange,ratio}]/candidates）；学生身份 `TutoringAuth.currentStudentId`

## 4. 聚合别名合并

- [x] 4.1 聚合 `aggregateTopic` 建新 CANDIDATE 前：`findByTopicLabelOrAlias` 命中 → 更新现有；未命中 → 与现有 CANDIDATE/STABLE 题型比 kp_uri 集合重叠 ≥ 阈值（默认 0.7）→ 插入别名 + 折叠本桶观测进 canonical（upsert 分布桶统计合并 + updateStats）；无相似 → 新建 CANDIDATE
- [x] 4.2 解析管线② `resolveByCatalog` 与 `recordStudentVote` 的题型库查询改 `findByTopicLabelOrAlias`（resolveByCatalog/buildCandidates 已改；vote 不经题型库查询，obs 由聚合按别名归并）
- [x] 4.3 配置项：`ai-edu.kp.aggregation.alias-overlap=0.7`（`application.yml` + 聚合服务 `@Value`）

## 5. 测试

- [x] 5.1 题目理解单测：参考词表注入、多候选解析、LLM 失败返回空（KpQuestionAnalyzerTest 4 用例）
- [x] 5.2 analyze-question 接口/应用测试：题型库命中返回全分布、未命中 resolve 单点、PENDING 携带候选（KpQuestionAnalysisAppServiceTest 4 用例）；纯分析不写 obs（resolveReadOnly 测试）；参数校验 10001 / 未登录 10004 / 非学生 20004（KpQuestionAnalysisControllerTest 4 用例）
- [x] 5.3 别名合并单测：kp 重叠合并 + 统计折叠、无相似新建、别名命中后续聚合不新建、变体不再稀释阈值（KpQuestionTypeAggregationServiceTest ALI 用例）
- [x] 5.4 回归：resolve/vote/题型库分页 + 关联知识点接口契约不变（全量 mvn test BUILD SUCCESS，6 模块全绿，1 skip=无关真 LLM 测试）

## 9. 联调修复 + 存疑挂起闭环（2026-08-17 联调后新增，已实现）

- [x] 9.1 [确定性] analyze 全候选遍历（顺序无关）+ prompt 收敛（参考词表强制优先/按把握排序/「无法识别」兜底）+ 数据锚优先；**移除缓存**（提示词 + 功能点，非缓存，用户拍板）；LLM 消歧预算前 2 个候选（冷启动最坏 3 次 LLM）
- [x] 9.2 [关联质量] 聚合 `findResolved`/`findResolvedByTopicLabels` **排除 WEAK**（LLM 幻觉不进题型库，防「对数方程求解」式污染）；`KpResolution` 加 `weak` 标记 → analyze 对 WEAK 降级为 PENDING 候选待确认
- [x] 9.3 [候选] analyze 候选镜像校验（`inMirror` 精确→LIKE，非镜像丢弃 → vote 不 10003）；WEAK 的 kpLabel 也进候选
- [x] 9.4 [vote 闭环] `resolvePendingByStudentTopic` 转正该生该题型 PENDING obs（待确认清单即时消失）；无 PENDING 才新建
- [x] 9.5 [挂起闭环] analyze 存疑落 `upsertPendingIfAbsent` PENDING obs（去重，进 pending-kps）；维护任务 `rejudgePending`：PENDING → LLM 重判 → 高置信转 WEAK → 共现转正
- [x] 9.6 [契约] analyze-question 用 `requireStudent`（未登录 10004 / 非学生 20004）；api.md 补 WEAK→PENDING/candidates 校验/vote 转正/30s 超时
- [x] 9.7 [测试] FIX-001~008 全绿（WEAK 降级/遍历/挂起/镜像校验/vote 转正/聚合排 WEAK/维护重判）；全量回归 BUILD SUCCESS

## 10. 封闭域约束选择：学段知识点池（P0 打通闭环，2026-08-17 第二轮，待实现）

- [x] 10.1 [领域] `KgKnowledgePointRepository.findLabelsByStage(stage)`：按学段取全量知识点 label 池（数学教材知识点，供约束选择）
- [x] 10.2 [端口] domain 新增 `KpConstrainedAssociationPort.associate(questionText, grade, pool)`：输入题目+年级+池 → 输出 top-N 池内 label（纯选择，不凭空猜）
- [x] 10.3 [infra] `KpConstrainedAssociator`（LLM 实现）：prompt 强制「只能从池里选 1-3 最相关，禁止输出池外，禁止说无法确定」；LLM 失败 → 回退池前 N 个（恒非空）
- [x] 10.4 [子池粗筛] analyze 取池后按题目 n-gram/题型名召回子池（池 >200 缩容）；召回空回退全池截断；grade→stage 用 `KgStageEnum`（primary/middle/high ↔ 小学/初中/高中）
- [x] 10.5 [池约束编排] `KpPoolAssociateService`（抽离保留）：学段池 → 粗筛 → LLM 从池选 top-N → top-1 落 RESOLVED obs。**analyze 本期未接线（前端降级：题库 miss → PENDING，空可接受）**；「题库和知识点」独立迭代启动时在 analyze ② 处接线即可
- [x] 10.6 [keyword 兜底] `POST /api/kg/knowledge-points` 支持 `keyword`：`WHERE label LIKE '%kw%'`（stage 过滤内，`findPageByStageAndKeyword`/`countByStageAndKeyword`）→ 前端 KpSearchSelector 空候选手动搜确认
- [x] 10.7 [测试] POOL-001~007 全绿（从池选/恒非空/池外过滤/池空兜底/analyze top-1 落观测/无年级兜底/keyword 过滤）；全量回归 BUILD SUCCESS

## 11. P1/P2 增强（非本次必须）

- [x] 11.1 [P1 稳定性] 封闭域池确定 + `KpConstrainedAssociator` 池内排序确定性（`sorted(pool.indexOf)`，LLM 顺序打乱不影响 top-1）；新增 `sortsByPoolOrder` 用例
- [x] 11.2 [P1 聚合手动触发] `POST /api/kp/aggregation/run`（ADMIN，`@PreAuthorize("hasRole('ADMIN')")`）→ `KpQuestionTypeAggregationService.aggregate()`，联调即时验证题型库沉淀；controller 测试覆盖
- [ ] 11.3 [P2 管理端审核] 学生题型 ↔ 年级知识点对照页 + LLM 批量关联 + 人工校准喂题型库（**独立功能点，暂缓，需产品立项**；现有 `KpAliasReviewController` 可作基础）

## 12. 图片题目多模态直看（2026-08-17 Python 拍板方案 B）

- [x] 12.1 [契约] `QuestionUnderstandRequest`/`QuestionUnderstandResult`（domain contract）：imageUrl/topicHint/grade → topicLabels/questionKps（camelCase，与 Python 约定一致）
- [x] 12.2 [Python 客户端] `TutoringLlmPort.understandQuestion` + `TutoringLlmClient` 实现（WebClient JSON → Python `/api/tutoring/question-understand`，retry + 30s 超时，失败降级空 → PENDING）；`TutoringConfig`/`TutoringProperties` 加 path/timeout
- [x] 12.3 [无会话上传] `KpQuestionAnalysisAppService.uploadAnalyzeImage`：COS 上传（路径 `tutoring/questions/{studentId}/analyze/{ts}.ext`，无 sessionId 依赖）+ 格式白名单校验 + 签名 URL
- [x] 12.4 [编排] `analyzeImage`：上传 → topicHint=findTopTopicLabels(20) → 调 Python 看图 → ①题型库命中权威 → ②questionKps 顺带展示（镜像校验）→ ③PENDING 挂起
- [x] 12.5 [端点] `POST /api/kp/analyze-question/image`（multipart file，STUDENT）；`analyze-question` DTO 一致
- [x] 12.6 [测试] image 编排 4 用例 + controller 2 用例全绿；全量回归 BUILD SUCCESS
