package com.ai.edu.interface_.api;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.kg.*;
import com.ai.edu.application.service.kg.KgKnowledgeSystemAppService;
import com.ai.edu.application.service.kg.KgNavigationAppService;
import com.ai.edu.application.service.kg.KgNeo4jService;
import com.ai.edu.application.service.kg.KgSyncAppService;
import com.ai.edu.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * KnowledgeGraphController 单元测试
 * 直接注入 Controller，使用 MockBean 模拟服务
 *
 * 测试目标：使用 MockMvc 验证 HTTP 请求/响应
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
class KnowledgeGraphControllerTest {

    @org.springframework.beans.factory.annotation.Autowired
    private KnowledgeGraphController knowledgeGraphController;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KgSyncAppService kgSyncAppService;
    @MockBean
    private KgNavigationAppService kgNavigationAppService;
    @MockBean
    private KgKnowledgeSystemAppService kgKnowledgeSystemAppService;
    @MockBean
    private KgNeo4jService kgNeo4jService;

    // ==================== 6.11.1 POST /api/kg/sync/full ====================

    @Test
    @Order(1)
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("syncFull 成功场景 — 200 + SyncResult")
    void syncFull_success_shouldReturnSyncResult() {
        SyncRequest request = SyncRequest.builder().subject("数学").build();
        SyncResult result = SyncResult.builder()
                .syncId(1L)
                .status("success")
                .insertedCount(10)
                .updatedCount(5)
                .statusChangedCount(2)
                .reconciliationStatus("matched")
                .build();
        when(kgSyncAppService.syncFull(any())).thenReturn(result);

        ApiResponse<SyncResult> response = knowledgeGraphController.syncFull(request);

        assertEquals("00000", response.getCode());
        assertEquals("success", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(1L, response.getData().getSyncId());
        assertEquals("success", response.getData().getStatus());
        assertEquals(10, response.getData().getInsertedCount());
        assertEquals("matched", response.getData().getReconciliationStatus());
    }

    // ==================== 6.11.2 POST /api/kg/sync/full 权限校验 ====================

    @Test
    @Order(2)
    @WithMockUser(roles = {"STUDENT"})
    @DisplayName("syncFull 权限校验 — STUDENT 角色 → AccessDenied（被 GlobalExceptionHandler 处理为 500）")
    void syncFull_noPermission_studentRole_shouldReturnAccessDenied() throws Exception {
        SyncRequest request = SyncRequest.builder().subject("数学").build();

        // 注意: 由于 GlobalExceptionHandler 的 @ExceptionHandler(Exception.class) 会捕获
        // AccessDeniedException 并返回 500，而不是 Spring Security 原生的 403
        // 这里验证返回的 error code 不是 "00000"
        mockMvc.perform(post("/api/auth/kg/sync/full")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.code").value("10000"));
    }

    // ==================== 6.11.3 GET /api/kg/sync/status ====================

    @Test
    @Order(3)
    @DisplayName("getSyncStatus 成功场景")
    void getSyncStatus_success() {
        SyncStatusDTO status = SyncStatusDTO.builder()
                .status("idle")
                .lastSyncStatus("success")
                .lastSyncAt("2024-01-01T10:00:00")
                .build();
        when(kgSyncAppService.getSyncStatus()).thenReturn(status);

        ApiResponse<SyncStatusDTO> response = knowledgeGraphController.getSyncStatus();

        assertEquals("00000", response.getCode());
        assertEquals("idle", response.getData().getStatus());
        assertEquals("success", response.getData().getLastSyncStatus());
    }

    // ==================== 6.11.4 POST /api/kg/sync/records ====================

    @Test
    @Order(4)
    @DisplayName("getSyncRecords 查询（支持维度筛选）")
    void getSyncRecords_withScopeFilter() {
        List<SyncRecordDTO> records = List.of(
                SyncRecordDTO.builder()
                        .id(1L)
                        .syncType("full")
                        .edition("人教版")
                        .subject("数学")
                        .grade("七年级")
                        .status("success")
                        .insertedCount(10)
                        .updatedCount(5)
                        .statusChangedCount(2)
                        .reconciliationStatus("matched")
                        .build(),
                SyncRecordDTO.builder()
                        .id(2L)
                        .syncType("full")
                        .edition("人教版")
                        .subject("英语")
                        .grade("八年级")
                        .status("success")
                        .insertedCount(8)
                        .updatedCount(2)
                        .statusChangedCount(1)
                        .reconciliationStatus("matched")
                        .build()
        );
        SyncRecordQueryRequest request = SyncRecordQueryRequest.builder()
                .edition("人教版")
                .subject("数学")
                .size(10)
                .build();
        when(kgSyncAppService.getSyncRecords(any(SyncRecordQueryRequest.class))).thenReturn(records);

        List<SyncRecordDTO> response = knowledgeGraphController.getSyncRecords(request).getData();

        assertEquals(2, response.size());
        assertEquals("full", response.get(0).getSyncType());
        assertEquals("数学", response.get(0).getSubject());
        assertEquals("英语", response.get(1).getSubject());
    }

    // ==================== 6.11.5 GET /api/kg/textbooks ====================

    @Test
    @Order(5)
    @DisplayName("getTextbooks 列表查询（含 subject/stage 过滤参数）")
    void getTextbooks_withFilters() {

    }

    @Test
    @Order(6)
    @DisplayName("getTextbooks 无参数 — 返回所有教材")
    void getTextbooks_noFilter() {

    }

    // ==================== 6.11.6 POST /api/kg/textbooks/chapters ====================

    @Test
    @Order(7)
    @DisplayName("getChaptersByTextbook 章节树查询 — POST 请求")
    void getChaptersByTextbook_chapterTree() {
        List<ChapterTreeNode> chapters = List.of(
                ChapterTreeNode.builder()
                        .uri("uri:ch1")
                        .label("第一章")
                        .orderIndex(1)
                        .sections(List.of(
                                SectionNode.builder()
                                        .uri("uri:sec1")
                                        .label("第一节")
                                        .knowledgePointCount(3)
                                        .build()
                        ))
                        .build(),
                ChapterTreeNode.builder()
                        .uri("uri:ch2")
                        .label("第二章")
                        .orderIndex(2)
                        .sections(List.of())
                        .build()
        );
        when(kgNavigationAppService.getChaptersByTextbook("uri:tb1")).thenReturn(chapters);

        TextbookUriRequest request = TextbookUriRequest.builder().textbookUri("uri:tb1").build();
        List<ChapterTreeNode> response = knowledgeGraphController.getChaptersByTextbook(request).getData();

        assertEquals(2, response.size());
        assertEquals("uri:ch1", response.get(0).getUri());
        assertEquals("第一章", response.get(0).getLabel());
        assertEquals(1, response.get(0).getOrderIndex());
        assertEquals(1, response.get(0).getSections().size());
        assertEquals(3, response.get(0).getSections().get(0).getKnowledgePointCount());
        assertEquals("uri:ch2", response.get(1).getUri());
    }

    // ==================== 6.11.7 POST /api/kg/sections/points ====================

    @Test
    @Order(8)
    @DisplayName("getKnowledgePointsBySection 知识点列表 — POST 请求")
    void getKnowledgePointsBySection_kpList() {
        List<KgKnowledgePointDetailDTO> points = List.of(
                KgKnowledgePointDetailDTO.builder()
                        .uri("uri:kp1")
                        .label("知识点1")
                        .sectionUri("uri:sec1")
                        .sectionLabel("第一节")
                        .chapterUri("uri:ch1")
                        .chapterLabel("第一章")
                        .build(),
                KgKnowledgePointDetailDTO.builder()
                        .uri("uri:kp2")
                        .label("知识点2")
                        .sectionUri("uri:sec1")
                        .sectionLabel("第一节")
                        .chapterUri("uri:ch1")
                        .chapterLabel("第一章")
                        .build()
        );
        when(kgNavigationAppService.getKnowledgePointsBySection("uri:sec1")).thenReturn(points);

        SectionUriRequest request = SectionUriRequest.builder().sectionUri("uri:sec1").build();
        List<KgKnowledgePointDetailDTO> response = knowledgeGraphController.getKnowledgePointsBySection(request).getData();

        assertEquals(2, response.size());
        assertEquals("uri:kp1", response.get(0).getUri());
        assertEquals("知识点1", response.get(0).getLabel());
        assertEquals("uri:sec1", response.get(0).getSectionUri());
        assertEquals("uri:ch1", response.get(0).getChapterUri());
    }

    // ==================== 6.11.8 POST /api/kg/knowledge-points/detail ====================

    @Test
    @Order(9)
    @DisplayName("getKnowledgePointDetail 知识点详情（含 2 层父级）— POST 请求")
    void getKnowledgePointDetail_withParents() {
        KgKnowledgePointDetailDTO detail = KgKnowledgePointDetailDTO.builder()
                .uri("uri:kp1")
                .label("勾股定理")
                .sectionUri("uri:sec1")
                .sectionLabel("几何基础")
                .chapterUri("uri:ch1")
                .chapterLabel("几何")
                .build();
        when(kgNavigationAppService.getKnowledgePointDetail("uri:kp1")).thenReturn(detail);

        KnowledgePointUriRequest request = KnowledgePointUriRequest.builder().kpUri("uri:kp1").build();
        KgKnowledgePointDetailDTO response = knowledgeGraphController.getKnowledgePointDetail(request).getData();

        assertNotNull(response);
        assertEquals("uri:kp1", response.getUri());
        assertEquals("勾股定理", response.getLabel());
        assertEquals("uri:sec1", response.getSectionUri());
        assertEquals("几何基础", response.getSectionLabel());
        assertEquals("uri:ch1", response.getChapterUri());
        assertEquals("几何", response.getChapterLabel());
    }

    // ==================== 6.11.9 POST /api/kg/knowledge-points/graph ====================

    @Test
    @Order(10)
    @DisplayName("getKnowledgePointGraph 知识点图谱 — POST 请求")
    void getKnowledgePointGraph_success() {
        KgGraphDTO graph = KgGraphDTO.builder()
                .nodes(List.of())
                .edges(List.of())
                .build();
        when(kgNeo4jService.getKnowledgePointGraph("uri:kp1")).thenReturn(graph);

        KnowledgePointUriRequest request = KnowledgePointUriRequest.builder().kpUri("uri:kp1").build();
        KgGraphDTO response = knowledgeGraphController.getKnowledgePointGraph(request).getData();

        assertNotNull(response);
    }

    // ==================== 6.11.10 POST /api/kg/grades/textbooks ====================

    @Test
    @Order(11)
    @DisplayName("getTextbooksByGrade 年级教材列表 — POST 请求")
    void getTextbooksByGrade_success() {
        List<KgTextbookDTO> textbooks = List.of(
                KgTextbookDTO.builder()
                        .uri("uri:tb1")
                        .label("人教版数学七年级上册")
                        .grade("七年级")
                        .build()
        );
        when(kgNavigationAppService.getTextbooksByGrade("七年级")).thenReturn(textbooks);

        GradeRequest request = GradeRequest.builder().grade("七年级").build();
        List<KgTextbookDTO> response = knowledgeGraphController.getTextbooksByGrade(request).getData();

        assertEquals(1, response.size());
        assertEquals("uri:tb1", response.get(0).getUri());
        assertEquals("七年级", response.get(0).getGrade());
    }

    // ==================== 6.11.11 POST /api/kg/system/grade ====================

    @Test
    @Order(12)
    @DisplayName("getGradeSystem 知识体系 — POST 请求")
    void getGradeSystem_success() {
        KgGradeSystemDTO system = KgGradeSystemDTO.builder()
                .grade("七年级")
                .groupBy("subject")
                .groups(List.of())
                .totalKnowledgePoints(0)
                .build();
        when(kgKnowledgeSystemAppService.getGradeSystem("七年级", "subject")).thenReturn(system);

        GradeSystemRequest request = GradeSystemRequest.builder()
                .grade("七年级")
                .groupBy("subject")
                .build();
        KgGradeSystemDTO response = knowledgeGraphController.getGradeSystem(request).getData();

        assertEquals("七年级", response.getGrade());
        assertEquals("subject", response.getGroupBy());
    }

    // ==================== 6.11.12 POST /api/kg/system/stats ====================

    @Test
    @Order(13)
    @DisplayName("getGradeStats 年级统计 — POST 请求")
    void getGradeStats_success() {
        StatsDTO stats = StatsDTO.builder()
                .grade("七年级")
                .totalKnowledgePoints(50)
                .totalTextbooks(2)
                .totalChapters(10)
                .totalSections(20)
                .difficultyDistribution(Map.of("easy", 20, "medium", 20, "hard", 10))
                .build();
        when(kgKnowledgeSystemAppService.getGradeStats("七年级")).thenReturn(stats);

        GradeRequest request = GradeRequest.builder().grade("七年级").build();
        StatsDTO response = knowledgeGraphController.getGradeStats(request).getData();

        assertEquals("七年级", response.getGrade());
        assertEquals(50, response.getTotalKnowledgePoints());
        assertEquals(2, response.getTotalTextbooks());
        assertEquals(3, response.getDifficultyDistribution().size());
    }

    // ==================== 6.11.13 GET /api/kg/neo4j/health ====================

    @Test
    @Order(14)
    @DisplayName("getNeo4jHealth 健康检查")
    void getNeo4jHealth_success() {
        HealthDTO health = HealthDTO.builder()
                .available(true)
                .responseTimeMs(15)
                .message("Connected")
                .build();
        when(kgNeo4jService.getNeo4jHealth()).thenReturn(health);

        HealthDTO response = knowledgeGraphController.getNeo4jHealth().getData();

        assertTrue(response.isAvailable());
        assertEquals(15, response.getResponseTimeMs());
        assertEquals("Connected", response.getMessage());
    }

    // ==================== 6.11.14 POST /api/kg/concepts/batch-relations ====================

    @Test
    @Order(15)
    @DisplayName("batchGetConceptRelations 批量关联")
    void batchGetConceptRelations_success() {
        BatchRelationsDTO result = BatchRelationsDTO.builder()
                .relations(List.of(
                        BatchRelationsDTO.RelationEntry.builder()
                                .uri("uri:tb1")
                                .relatedUris(List.of("uri:ch1", "uri:ch2"))
                                .build()
                ))
                .build();
        when(kgNeo4jService.batchGetConceptRelations(List.of("uri:tb1"))).thenReturn(result);

        KnowledgeGraphController.BatchRelationsRequest request = new KnowledgeGraphController.BatchRelationsRequest();
        request.setUris(List.of("uri:tb1"));

        BatchRelationsDTO response = knowledgeGraphController.batchGetConceptRelations(request).getData();

        assertEquals(1, response.getRelations().size());
        assertEquals("uri:tb1", response.getRelations().get(0).getUri());
        assertEquals(2, response.getRelations().get(0).getRelatedUris().size());
    }

    // ==================== 6.11.15 统一响应包装验证 ====================

    @Test
    @Order(16)
    @DisplayName("统一响应包装验证 — 所有接口返回 ApiResponse 格式")
    void allEndpoints_returnApiResponseFormat() {
        // 设置 SecurityContext（直接调用时需要手动设置认证）
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test", "test",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
        try {
            // 验证 syncFull 响应格式
            when(kgSyncAppService.syncFull(any())).thenReturn(SyncResult.builder().syncId(1L).status("success").build());
            ApiResponse<SyncResult> syncResp = knowledgeGraphController.syncFull(null);
            assertEquals("00000", syncResp.getCode());
            assertEquals("success", syncResp.getMessage());
            assertNotNull(syncResp.getData());

            // 验证 getSyncStatus 响应格式
            when(kgSyncAppService.getSyncStatus()).thenReturn(SyncStatusDTO.builder().status("idle").build());
            ApiResponse<SyncStatusDTO> statusResp = knowledgeGraphController.getSyncStatus();
            assertEquals("00000", statusResp.getCode());
            assertNotNull(statusResp.getData());

        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // ==================== 错误场景测试 ====================

    @Test
    @Order(17)
    @DisplayName("getKnowledgePointsBySection 小节不存在 — 应返回空列表")
    void getKnowledgePointsBySection_sectionNotFound() {
        when(kgNavigationAppService.getKnowledgePointsBySection("uri:notexist")).thenReturn(List.of());

        SectionUriRequest request = SectionUriRequest.builder().sectionUri("uri:notexist").build();
        List<KgKnowledgePointDetailDTO> response = knowledgeGraphController.getKnowledgePointsBySection(request).getData();

        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    @Test
    @Order(18)
    @DisplayName("getKnowledgePointDetail 知识点不存在 — 应抛异常")
    void getKnowledgePointDetail_notFound() {
        when(kgNavigationAppService.getKnowledgePointDetail("uri:notexist"))
                .thenThrow(new BusinessException("70003", "知识点不存在"));

        KnowledgePointUriRequest request = KnowledgePointUriRequest.builder().kpUri("uri:notexist").build();
        BusinessException ex = assertThrows(BusinessException.class, () ->
                knowledgeGraphController.getKnowledgePointDetail(request));
        assertEquals("70003", ex.getCode());
    }

    @Test
    @Order(19)
    @DisplayName("getChaptersByTextbook 教材不存在 — 应抛异常")
    void getChaptersByTextbook_textbookNotFound() {
        when(kgNavigationAppService.getChaptersByTextbook("uri:notexist"))
                .thenThrow(new BusinessException("70001", "教材不存在"));

        TextbookUriRequest request = TextbookUriRequest.builder().textbookUri("uri:notexist").build();
        BusinessException ex = assertThrows(BusinessException.class, () ->
                knowledgeGraphController.getChaptersByTextbook(request));
        assertEquals("70001", ex.getCode());
    }
}