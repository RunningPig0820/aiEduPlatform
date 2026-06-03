# 知识图谱 测试用例设计

## 1. 测试概述

### 1.1 测试目标
验证知识图谱同步、导航查询、知识体系构建的所有 REST API，确保数据同步正确性和查询准确性。

### 1.2 测试方式
- **集成测试**：使用 `@SpringBootTest` + `@AutoConfigureMockMvc`
- **MySQL 回滚**：使用 `@Transactional` + `@Rollback`
- **Neo4j Mock**：Mock Neo4j Driver，返回预定义的图数据

### 1.3 测试环境配置
- Profile: `test`
- 数据库：使用测试 MySQL（事务回滚）
- Neo4j: Mock 模式

---

## 2. 测试数据

### 2.1 教材测试数据

| 参数 | 值 | 说明 |
|-----|-----|-----|
| TEXTBOOK_URI | "http://edukg.org/knowledge/3.1/textbook/一年级上册" | 教材 URI |
| TEXTBOOK_LABEL | "一年级上册" | 教材名称 |
| TEXTBOOK_GRADE | "一年级" | 年级 |
| TEXTBOOK_PHASE | "primary" | 学段 |
| TEXTBOOK_SUBJECT | "math" | 学科 |

### 2.2 章节测试数据

| 参数 | 值 | 说明 |
|-----|-----|-----|
| CHAPTER_URI | "http://edukg.org/knowledge/3.1/chapter/准备课" | 章节 URI |
| CHAPTER_LABEL | "准备课" | 章节名称 |
| CHAPTER_TOPIC | "数与代数" | 专题 |

### 2.3 小节测试数据

| 参数 | 值 | 说明 |
|-----|-----|-----|
| SECTION_URI | "http://edukg.org/knowledge/3.1/section/10以内数的认识" | 小节 URI |
| SECTION_LABEL | "10以内数的认识" | 小节名称 |

### 2.4 知识点测试数据

| 参数 | 值 | 说明 |
|-----|-----|-----|
| KP_URI | "http://edukg.org/knowledge/3.1/kp/加法" | 知识点 URI |
| KP_LABEL | "加法" | 知识点名称 |
| KP_DIFFICULTY | "easy" | 难度 |
| KP_IMPORTANCE | "high" | 重要性 |
| KP_COGNITIVE_LEVEL | "理解" | 认知层级 |

---

## 3. 测试用例清单

### 3.1 同步功能 (KG-SYNC-001 ~ KG-SYNC-014)

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| KG-SYNC-001 | 触发全量同步-成功 | Neo4j 有数据 | 无 | code=00000, status="running" |
| KG-SYNC-002 | 同步完成-新增知识点 | Neo4j 有 MySQL 中没有的 KP | 无 | insertedCount > 0, MySQL 中新增记录 |
| KG-SYNC-003 | 同步完成-更新知识点属性 | Neo4j 中 KP 属性变化 | 无 | updatedCount > 0, MySQL 中记录已更新 |
| KG-SYNC-004 | 同步完成-状态变更知识点 | Neo4j 中删除了 KP | 无 | statusChangedCount > 0, MySQL 中 status='deleted' |
| KG-SYNC-005 | 同步完成-数据完整性 | 同步完成 | 无 | 所有非 is_deleted 记录与 Neo4j 一致 |
| KG-SYNC-006 | 同步中再次触发 | 同步正在进行 | 无 | code=70006, message="同步正在进行" |
| KG-SYNC-007 | 查询同步状态-运行中 | 同步执行中 | 无 | status="running", processedNodes > 0 |
| KG-SYNC-008 | 查询同步状态-空闲 | 无同步任务 | 无 | status="idle", lastSync 含 insertedCount/updatedCount/statusChangedCount |
| KG-SYNC-009 | 定向同步-按年级 | 有多年级数据 | body={"grade":"一年级"} | 仅同步一年级数据，scope 记录范围 |
| KG-SYNC-010 | 定向同步-按教材URI | 有多本教材 | body={"textbookUri":"xxx"} | 仅同步指定教材 |
| KG-SYNC-011 | 同步对账-一致 | 同步完成 | 无 | reconciliationStatus="matched" |
| KG-SYNC-012 | 同步对账-不一致 | Neo4j 数据与 MySQL 不一致 | 无 | reconciliationStatus="mismatched", details 记录差异 |
| KG-SYNC-013 | URI 校验-非法 URI | Neo4j 返回空 URI | 无 | 跳过该节点，记录到 details，继续处理其他节点 |
| KG-SYNC-014 | 同步失败-事务回滚 | 同步中途 Neo4j 断开 | 无 | 事务回滚，MySQL 无半清空状态，前端仍看到旧数据 |

### 3.2 教材列表 (KG-NAV-001 ~ KG-NAV-003)

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| KG-NAV-001 | 获取所有教材 | 已同步数据 | 无 | 返回 23 条教材记录 |
| KG-NAV-002 | 按学段过滤 | 有 primary/middle 教材 | phase=primary | 仅返回 primary 教材 |
| KG-NAV-003 | 按学科过滤 | 仅 math 数据 | subject=math | 返回 math 教材 |

### 3.3 教材章节树 (KG-NAV-004 ~ KG-NAV-007)

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| KG-NAV-004 | 获取章节树-成功 | textbook 有章节关联 | textbookUri=url_encoded | 返回 chapters 嵌套 sections，含 orderIndex |
| KG-NAV-005 | 获取章节树-教材不存在 | 教材不存在 | textbookUri=unknown | code=70001 |
| KG-NAV-006 | 验证 KP 数量 | 小节有知识点关联 | textbookUri=url_encoded | 每个 section 有 knowledgePointCount |
| KG-NAV-007 | 空章节自动过滤 | 有章节无小节 | textbookUri=url_encoded | 返回结果中不包含空章节 |

### 3.4 小节知识点 (KG-NAV-008 ~ KG-NAV-010)

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| KG-NAV-008 | 获取知识点-成功 | section 有 KP 关联 | sectionUri=url_encoded | 返回知识点列表，uri 字段非空 |
| KG-NAV-009 | 获取知识点-小节不存在 | 小节不存在 | sectionUri=unknown | code=70004 |
| KG-NAV-010 | 获取知识点-空小节 | 小节无 KP 关联 | sectionUri=url_encoded_empty | knowledgePoints 为空数组 |

### 3.5 知识点详情 (KG-NAV-011 ~ KG-NAV-015)

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| KG-NAV-011 | 获取详情-成功 | kp_1 有完整父级 | kpUri=url_encoded | 返回 uri/label/difficulty/importance/sectionLabel/chapterLabel |
| KG-NAV-012 | 获取详情-2层父级验证 | kp_1 有完整父级 | kpUri=url_encoded | 返回 sectionLabel 和 chapterLabel，不返回 textbookLabel/grade |
| KG-NAV-013 | 获取详情-知识点不存在 | 知识点不存在 | kpUri=unknown | code=70003 |
| KG-NAV-014 | 获取详情-已删除知识点 | kp_100 status='deleted' | kpUri=url_encoded_deleted | code=70003（status≠active 视为不存在） |
| KG-NAV-015 | 获取详情-已合并知识点 | kp_200 status='merged' | kpUri=url_encoded_merged | code=70003（status≠active 视为不存在） |

### 3.6 Neo4j 健康与批量查询 (KG-NEO4J-001 ~ KG-NEO4J-004)

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| KG-NEO4J-001 | 健康检查-可用 | Neo4j 正常 | 无 | available=true, responseTimeMs > 0 |
| KG-NEO4J-002 | 健康检查-不可用 | Neo4j 断开 | 无 | available=false, error 含错误信息，HTTP 200 |
| KG-NEO4J-003 | 批量查询关联-成功 | Neo4j 有数据 | body={"uris":["uri1","uri2"]} | 返回每个 URI 的关联列表 |
| KG-NEO4J-004 | 批量查询-降级 | Neo4j 不可用 | body={"uris":["uri1"]} | neo4jAvailable=false, relations 为空 |

### 3.6 年级知识体系 (KG-SYS-001 ~ KG-SYS-005)

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| KG-SYS-001 | 获取知识体系-按教材分组 | 一年级有数据 | grade=一年级 | 返回按 textbook 分组的完整结构 |
| KG-SYS-002 | 获取知识体系-按专题分组 | 有专题数据 | grade=一年级&groupBy=topic | 返回按 topic 分组的结构 |
| KG-SYS-003 | 获取知识体系-年级不存在 | 年级无数据 | grade=未知年级 | 返回空结构，totalKnowledgePoints=0 |
| KG-SYS-004 | 验证知识点嵌套 | 一年级有知识点 | grade=一年级 | 知识点包含 difficulty/importance/cognitiveLevel |
| KG-SYS-005 | 验证知识点总数 | 一年级多个教材 | grade=一年级 | totalKnowledgePoints = 所有知识点之和 |

### 3.7 年级统计 (KG-SYS-006 ~ KG-SYS-009)

| 用例编号 | 场景描述 | 前置条件 | 输入 | 预期结果 |
|---------|---------|---------|------|---------|
| KG-SYS-006 | 获取统计-成功 | 一年级有数据 | grade=一年级 | 返回 totalKnowledgePoints 和各分布 |
| KG-SYS-007 | 难度分布验证 | 有 easy/medium/hard | grade=一年级 | difficultyDistribution 各值 > 0 或合理 |
| KG-SYS-008 | 认知层级分布验证 | 有多种认知层级 | grade=一年级 | cognitiveLevelDistribution 含记忆/理解/应用 |
| KG-SYS-009 | 认知层级覆盖验证 | 有 4 种认知层级 | grade=一年级 | cognitiveLevelDistribution 含记忆/理解/应用/分析 |

---

## 4. 错误码对照表

| 错误码 | 常量名 | 说明 |
|-------|-------|------|
| 00000 | SUCCESS | 成功 |
| 10001 | PARAM_ERROR | 参数错误 |
| 10004 | UNAUTHORIZED | 未登录 |
| 20004 | PERMISSION_DENIED | 权限不足 |
| 70001 | KG_TEXTBOOK_NOT_FOUND | 教材不存在 |
| 70002 | KG_CHAPTER_NOT_FOUND | 章节不存在 |
| 70003 | KG_KP_NOT_FOUND | 知识点不存在 |
| 70004 | KG_SECTION_NOT_FOUND | 小节不存在 |
| 70005 | KG_NEO4J_QUERY_ERROR | Neo4j 查询失败 |
| 70006 | KG_SYNC_IN_PROGRESS | 同步正在进行 |
| 70007 | KG_SYNC_PARAM_ERROR | 同步参数错误 |

---

## 5. 测试用例统计

| 模块 | 用例数量 |
|-----|---------|
| 同步功能 | 14 |
| 教材列表 | 3 |
| 教材章节树 | 4 |
| 小节知识点 | 3 |
| 知识点详情 | 5 |
| Neo4j 健康与批量查询 | 4 |
| 年级知识体系 | 5 |
| 年级统计 | 4 |
| **总计** | **42** |

---

## 6. 测试执行顺序

```
KG-SYNC-001 ~ KG-SYNC-014 : 同步功能测试
KG-NAV-001  ~ KG-NAV-015  : 导航查询测试
KG-NEO4J-001~ KG-NEO4J-004: Neo4j 健康与批量查询测试
KG-SYS-001  ~ KG-SYS-009  : 知识体系测试
```

---

## 7. 辅助方法

### 7.1 创建测试教材数据
```java
private KgTextbook createTestTextbook() {
    KgTextbook textbook = new KgTextbook();
    textbook.setUri("http://edukg.org/knowledge/3.1/textbook/一年级上册");
    textbook.setLabel("一年级上册");
    textbook.setGrade("一年级");
    textbook.setPhase("primary");
    textbook.setSubject("math");
    return kgTextbookRepository.save(textbook);
}
```

### 7.2 创建 Mock Neo4j 数据
```java
private void mockNeo4jTextbooks() {
    when(neo4jDriver.executeQuery("MATCH (t:Textbook) RETURN t.uri, t.label, t.grade, t.phase"))
        .thenReturn(List.of(
            Map.of("t.uri", "http://edukg.org/knowledge/3.1/textbook/一年级上册",
                   "t.label", "一年级上册",
                   "t.grade", "一年级", "t.phase", "primary")
        ));
}
```

### 7.3 验证同步后 MySQL 数据
```java
private void verifySyncedData() {
    assertThat(kgTextbookRepository.count()).isEqualTo(23);
    assertThat(kgChapterRepository.count()).isEqualTo(148);
    assertThat(kgSectionRepository.count()).isEqualTo(580);
    assertThat(kgKnowledgePointRepository.count()).isEqualTo(1740);
}
```

---

## 8. 运行测试

```bash
# 运行知识图谱所有测试
cd ai-edu-backend && mvn test -Dtest=Kg*Test

# 运行同步测试
mvn test -Dtest=KgSyncAppServiceTest

# 运行 Controller 测试
mvn test -pl ai-edu-interface -Dtest=KnowledgeGraphControllerTest
```
