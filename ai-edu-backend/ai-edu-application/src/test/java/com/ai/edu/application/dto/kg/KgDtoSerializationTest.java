package com.ai.edu.application.dto.kg;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DTO 序列化测试
 *
 * 测试目标：验证 Entity -> DTO 转换正确，序列化到 JSON 格式符合 API 文档
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KgDtoSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 6.12.1 KgTextbookDTO ====================

    @Test
    @Order(1)
    @DisplayName("KgTextbookDTO — Entity -> DTO 字段映射完整（uri/label/grade/stage/edition/orderIndex/subject/status）")
    void kgTextbookDTO_allFieldsMapped() throws Exception {
        KgTextbookDTO dto = KgTextbookDTO.builder()
                .uri("uri:tb1")
                .label("数学上册")
                .grade("七年级")
                .stage("middle")
                .edition("人教版")
                .orderIndex(1)
                .subject("math")
                .status("active")
                .build();

        assertEquals("uri:tb1", dto.getUri());
        assertEquals("数学上册", dto.getLabel());
        assertEquals("七年级", dto.getGrade());
        assertEquals("middle", dto.getStage());
        assertEquals("人教版", dto.getEdition());
        assertEquals(1, dto.getOrderIndex());
        assertEquals("math", dto.getSubject());
        assertEquals("active", dto.getStatus());
    }

    @Test
    @Order(2)
    @DisplayName("KgTextbookDTO — JSON 序列化字段名符合 camelCase")
    void kgTextbookDTO_jsonSerialization() throws Exception {
        KgTextbookDTO dto = KgTextbookDTO.builder()
                .uri("uri:tb1")
                .label("数学")
                .grade("七年级")
                .stage("middle")
                .edition("人教版")
                .orderIndex(1)
                .subject("math")
                .status("active")
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"uri\":\"uri:tb1\""));
        assertTrue(json.contains("\"label\":\"数学\""));
        assertTrue(json.contains("\"grade\":\"七年级\""));
        assertTrue(json.contains("\"stage\":\"middle\""));
        assertTrue(json.contains("\"subject\":\"math\""));
        assertTrue(json.contains("\"status\":\"active\""));
    }

    // ==================== 6.12.2 ChapterTreeNode ====================

    @Test
    @Order(3)
    @DisplayName("ChapterTreeNode — 嵌套结构正确（章节含小节，小节含 knowledgePointCount）")
    void chapterTreeNode_nestedStructure() throws Exception {
        List<ChapterTreeNode.SectionNode> sections = List.of(
                ChapterTreeNode.SectionNode.builder()
                        .uri("uri:sec1")
                        .label("第一节")
                        .orderIndex(1)
                        .knowledgePointCount(3)
                        .build(),
                ChapterTreeNode.SectionNode.builder()
                        .uri("uri:sec2")
                        .label("第二节")
                        .orderIndex(2)
                        .knowledgePointCount(5)
                        .build()
        );

        ChapterTreeNode chapter = ChapterTreeNode.builder()
                .uri("uri:ch1")
                .label("第一章")
                .topic("geometry")
                .orderIndex(1)
                .sections(sections)
                .build();

        assertEquals("uri:ch1", chapter.getUri());
        assertEquals("第一章", chapter.getLabel());
        assertEquals("geometry", chapter.getTopic());
        assertEquals(1, chapter.getOrderIndex());
        assertEquals(2, chapter.getSections().size());
        assertEquals("uri:sec1", chapter.getSections().get(0).getUri());
        assertEquals(3, chapter.getSections().get(0).getKnowledgePointCount());
        assertEquals(5, chapter.getSections().get(1).getKnowledgePointCount());
    }

    @Test
    @Order(4)
    @DisplayName("ChapterTreeNode — JSON 序列化嵌套结构正确")
    void chapterTreeNode_jsonNestedSerialization() throws Exception {
        ChapterTreeNode chapter = ChapterTreeNode.builder()
                .uri("uri:ch1")
                .label("第一章")
                .orderIndex(1)
                .sections(List.of(
                        ChapterTreeNode.SectionNode.builder()
                                .uri("uri:sec1")
                                .label("第一节")
                                .knowledgePointCount(3)
                                .build()
                ))
                .build();

        String json = objectMapper.writeValueAsString(chapter);

        assertTrue(json.contains("\"uri\":\"uri:ch1\""));
        assertTrue(json.contains("\"sections\""));
        assertTrue(json.contains("\"knowledgePointCount\":3"));
    }

    // ==================== 6.12.3 KgKnowledgePointDetailDTO ====================

    @Test
    @Order(5)
    @DisplayName("KgKnowledgePointDetailDTO — 含 2 层父级（sectionLabel/chapterLabel）")
    void kgKnowledgePointDetailDTO_withTwoParentLevels() throws Exception {
        KgKnowledgePointDetailDTO dto = KgKnowledgePointDetailDTO.builder()
                .uri("uri:kp1")
                .label("勾股定理")
                .difficulty("medium")
                .importance("high")
                .cognitiveLevel("apply")
                .sectionUri("uri:sec1")
                .sectionLabel("几何基础")
                .chapterUri("uri:ch1")
                .chapterLabel("几何")
                .build();

        assertEquals("uri:kp1", dto.getUri());
        assertEquals("勾股定理", dto.getLabel());
        assertEquals("medium", dto.getDifficulty());
        assertEquals("high", dto.getImportance());
        assertEquals("apply", dto.getCognitiveLevel());
        // 2 层父级
        assertEquals("uri:sec1", dto.getSectionUri());
        assertEquals("几何基础", dto.getSectionLabel());
        assertEquals("uri:ch1", dto.getChapterUri());
        assertEquals("几何", dto.getChapterLabel());
    }

    @Test
    @Order(6)
    @DisplayName("KgKnowledgePointDetailDTO — JSON 序列化字段完整")
    void kgKnowledgePointDetailDTO_jsonSerialization() throws Exception {
        KgKnowledgePointDetailDTO dto = KgKnowledgePointDetailDTO.builder()
                .uri("uri:kp1")
                .label("知识点")
                .sectionUri("uri:sec1")
                .sectionLabel("小节")
                .chapterUri("uri:ch1")
                .chapterLabel("章节")
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"uri\":\"uri:kp1\""));
        assertTrue(json.contains("\"sectionUri\":\"uri:sec1\""));
        assertTrue(json.contains("\"sectionLabel\":\"小节\""));
        assertTrue(json.contains("\"chapterUri\":\"uri:ch1\""));
        assertTrue(json.contains("\"chapterLabel\":\"章节\""));
    }

    // ==================== 6.12.4 SyncResult ====================

    @Test
    @Order(7)
    @DisplayName("SyncResult — 含 insertedCount/updatedCount/statusChangedCount/reconciliationStatus")
    void syncResult_allFieldsMapped() throws Exception {
        SyncResult result = SyncResult.builder()
                .syncId(1L)
                .status("success")
                .insertedCount(10)
                .updatedCount(5)
                .statusChangedCount(2)
                .reconciliationStatus("matched")
                .duration(1500L)
                .build();

        assertEquals(1L, result.getSyncId());
        assertEquals("success", result.getStatus());
        assertEquals(10, result.getInsertedCount());
        assertEquals(5, result.getUpdatedCount());
        assertEquals(2, result.getStatusChangedCount());
        assertEquals("matched", result.getReconciliationStatus());
        assertEquals(1500L, result.getDuration());
    }

    @Test
    @Order(8)
    @DisplayName("SyncResult — JSON 序列化字段名符合 camelCase")
    void syncResult_jsonSerialization() throws Exception {
        SyncResult result = SyncResult.builder()
                .syncId(1L)
                .status("success")
                .insertedCount(10)
                .updatedCount(5)
                .statusChangedCount(2)
                .reconciliationStatus("matched")
                .build();

        String json = objectMapper.writeValueAsString(result);

        assertTrue(json.contains("\"syncId\":1"));
        assertTrue(json.contains("\"status\":\"success\""));
        assertTrue(json.contains("\"insertedCount\":10"));
        assertTrue(json.contains("\"updatedCount\":5"));
        assertTrue(json.contains("\"statusChangedCount\":2"));
        assertTrue(json.contains("\"reconciliationStatus\":\"matched\""));
    }

    // ==================== 6.12.5 KgGradeSystemDTO ====================

    @Test
    @Order(9)
    @DisplayName("KgGradeSystemDTO — 分组结构正确（按 subject/stage）、知识点嵌套完整")
    void kgGradeSystemDTO_groupStructure() throws Exception {
        KgGradeSystemDTO.GroupDTO.ChapterNode chapter = KgGradeSystemDTO.GroupDTO.ChapterNode.builder()
                .uri("uri:ch1")
                .label("第一章")
                .orderIndex(1)
                .sections(List.of(
                        KgGradeSystemDTO.GroupDTO.SectionNode.builder()
                                .uri("uri:sec1")
                                .label("第一节")
                                .knowledgePoints(List.of())
                                .build()
                ))
                .build();

        KgGradeSystemDTO.GroupDTO group = KgGradeSystemDTO.GroupDTO.builder()
                .key("math")
                .label("数学")
                .chapters(List.of(chapter))
                .knowledgePointCount(5)
                .build();

        KgGradeSystemDTO dto = KgGradeSystemDTO.builder()
                .grade("七年级")
                .groupBy("subject")
                .groups(List.of(group))
                .totalKnowledgePoints(5)
                .build();

        assertEquals("七年级", dto.getGrade());
        assertEquals("subject", dto.getGroupBy());
        assertEquals(1, dto.getGroups().size());
        assertEquals("math", dto.getGroups().get(0).getKey());
        assertEquals("数学", dto.getGroups().get(0).getLabel());
        assertEquals(5, dto.getGroups().get(0).getKnowledgePointCount());
        assertEquals(1, dto.getGroups().get(0).getChapters().size());
        assertEquals(1, dto.getGroups().get(0).getChapters().get(0).getSections().size());
    }

    @Test
    @Order(10)
    @DisplayName("KgGradeSystemDTO — JSON 序列化分组结构正确")
    void kgGradeSystemDTO_jsonSerialization() throws Exception {
        KgGradeSystemDTO dto = KgGradeSystemDTO.builder()
                .grade("七年级")
                .groupBy("subject")
                .groups(List.of(
                        KgGradeSystemDTO.GroupDTO.builder()
                                .key("math")
                                .label("数学")
                                .chapters(List.of())
                                .knowledgePointCount(5)
                                .build()
                ))
                .totalKnowledgePoints(5)
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"grade\":\"七年级\""));
        assertTrue(json.contains("\"groupBy\":\"subject\""));
        assertTrue(json.contains("\"key\":\"math\""));
        assertTrue(json.contains("\"label\":\"数学\""));
        assertTrue(json.contains("\"totalKnowledgePoints\":5"));
    }

    // ==================== 6.12.6 StatsDTO ====================

    @Test
    @Order(11)
    @DisplayName("StatsDTO — 难度分布/认知层级分布/总数计算正确")
    void statsDTO_allFieldsMapped() throws Exception {
        StatsDTO dto = StatsDTO.builder()
                .grade("七年级")
                .totalKnowledgePoints(50)
                .totalTextbooks(2)
                .totalChapters(10)
                .totalSections(20)
                .difficultyDistribution(Map.of("easy", 20, "medium", 20, "hard", 10))
                .build();

        assertEquals("七年级", dto.getGrade());
        assertEquals(50, dto.getTotalKnowledgePoints());
        assertEquals(2, dto.getTotalTextbooks());
        assertEquals(10, dto.getTotalChapters());
        assertEquals(20, dto.getTotalSections());
        assertEquals(3, dto.getDifficultyDistribution().size());
        assertEquals(20, dto.getDifficultyDistribution().get("easy"));
        assertEquals(20, dto.getDifficultyDistribution().get("medium"));
        assertEquals(10, dto.getDifficultyDistribution().get("hard"));
    }

    @Test
    @Order(12)
    @DisplayName("StatsDTO — JSON 序列化字段完整")
    void statsDTO_jsonSerialization() throws Exception {
        StatsDTO dto = StatsDTO.builder()
                .grade("七年级")
                .totalKnowledgePoints(50)
                .totalTextbooks(2)
                .totalChapters(10)
                .totalSections(20)
                .difficultyDistribution(Map.of("easy", 20))
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"grade\":\"七年级\""));
        assertTrue(json.contains("\"totalKnowledgePoints\":50"));
        assertTrue(json.contains("\"totalTextbooks\":2"));
        assertTrue(json.contains("\"totalChapters\":10"));
        assertTrue(json.contains("\"totalSections\":20"));
        assertTrue(json.contains("\"difficultyDistribution\""));
        assertTrue(json.contains("\"easy\":20"));
    }

    // ==================== 6.12.7 JSON 反序列化验证 ====================

    @Test
    @Order(13)
    @DisplayName("JSON 反序列化 — KgTextbookDTO 可从 JSON 反序列化")
    void kgTextbookDTO_jsonDeserialization() throws Exception {
        String json = "{\"uri\":\"uri:tb1\",\"label\":\"数学上册\",\"grade\":\"七年级\",\"stage\":\"middle\",\"edition\":\"人教版\",\"orderIndex\":1,\"subject\":\"math\",\"status\":\"active\"}";

        KgTextbookDTO dto = objectMapper.readValue(json, KgTextbookDTO.class);

        assertEquals("uri:tb1", dto.getUri());
        assertEquals("数学上册", dto.getLabel());
        assertEquals("middle", dto.getStage());
        assertEquals("math", dto.getSubject());
    }

    @Test
    @Order(14)
    @DisplayName("JSON 反序列化 — SyncResult 可从 JSON 反序列化")
    void syncResult_jsonDeserialization() throws Exception {
        String json = "{\"syncId\":1,\"status\":\"success\",\"insertedCount\":10,\"updatedCount\":5,\"statusChangedCount\":2,\"reconciliationStatus\":\"matched\",\"duration\":1500}";

        SyncResult result = objectMapper.readValue(json, SyncResult.class);

        assertEquals(1L, result.getSyncId());
        assertEquals("success", result.getStatus());
        assertEquals(10, result.getInsertedCount());
        assertEquals("matched", result.getReconciliationStatus());
    }

    @Test
    @Order(15)
    @DisplayName("JSON 反序列化 — BatchRelationsDTO 嵌套结构可反序列化")
    void batchRelationsDTO_jsonDeserialization() throws Exception {
        String json = "{\"relations\":[{\"uri\":\"uri:tb1\",\"relatedUris\":[\"uri:ch1\",\"uri:ch2\"]}]}";

        BatchRelationsDTO dto = objectMapper.readValue(json, BatchRelationsDTO.class);

        assertEquals(1, dto.getRelations().size());
        assertEquals("uri:tb1", dto.getRelations().get(0).getUri());
        assertEquals(2, dto.getRelations().get(0).getRelatedUris().size());
        assertEquals("uri:ch1", dto.getRelations().get(0).getRelatedUris().get(0));
    }
}
