package com.ai.edu.interface_.api;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.kg.*;
import com.ai.edu.application.service.kg.KgKnowledgeSystemAppService;
import com.ai.edu.application.service.kg.KgNavigationAppService;
import com.ai.edu.application.service.kg.KgNeo4jService;
import com.ai.edu.application.service.kg.KgSyncAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识图谱 API 控制器
 * 基础路径: /api/kg
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/kg")
public class KnowledgeGraphController {

    @Resource
    private KgSyncAppService kgSyncAppService;
    @Resource
    private KgNavigationAppService kgNavigationAppService;
    @Resource
    private KgKnowledgeSystemAppService kgKnowledgeSystemAppService;
    @Resource
    private KgNeo4jService kgNeo4jService;

    // ==================== 同步接口 ====================

    /**
     * 5.2 全量同步
     * POST /api/kg/sync/full
     */
    @PostMapping("/sync/full")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ApiResponse<SyncResult> syncFull(@RequestBody(required = false) SyncRequest request) {
        log.info("知识图谱全量同步请求: {}", request);
        SyncResult result = kgSyncAppService.syncFull(request);
        return ApiResponse.success(result);
    }

    /**
     * 单独同步教材节点（不同步章节/小节/知识点）
     * POST /api/auth/kg/sync/textbooks
     */
    @PostMapping("/sync/textbooks")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ApiResponse<SyncResult> syncTextbooksOnly(@RequestBody(required = false) SyncRequest request) {
        log.info("知识图谱教材同步请求: {}", request);
        SyncResult result = kgSyncAppService.syncTextbooksOnly(request);
        return ApiResponse.success(result);
    }

    /**
     * 5.3 同步状态查询
     * GET /api/kg/sync/status
     */
    @GetMapping("/sync/status")
    public ApiResponse<SyncStatusDTO> getSyncStatus() {
        log.info("查询同步状态");
        SyncStatusDTO status = kgSyncAppService.getSyncStatus();
        return ApiResponse.success(status);
    }

    /**
     * 5.4 同步历史记录查询
     * POST /api/kg/sync/records
     */
    @PostMapping("/sync/records")
    public ApiResponse<List<SyncRecordDTO>> getSyncRecords(@RequestBody(required = false) SyncRecordQueryRequest request) {
        if (request == null) {
            request = SyncRecordQueryRequest.builder().page(1).size(10).build();
        }
        log.info("查询同步历史: edition={}, subject={}, stage={}, grade={}, size={}",
                request.getEdition(), request.getSubject(), request.getStage(), request.getGrade(), request.getSize());
        List<SyncRecordDTO> records = kgSyncAppService.getSyncRecords(request);
        return ApiResponse.success(records);
    }

    // ==================== 教材/章节导航 ====================


    /**
     * 5.6 教材章节树
     * POST /api/kg/textbooks/chapters
     *
     * @param request 教材 URI，格式示例：
     *   {"textbookUri": "http://edukg.org/knowledge/3.1/textbook/math#renjiao-g1s"}
     *   其中 renjiao-g1s 表示人教版一年级上册
     */
    @PostMapping("/textbooks/chapters")
    public ApiResponse<List<ChapterTreeNode>> getChaptersByTextbook(
            @RequestBody TextbookUriRequest request) {
        log.info("获取教材章节树: textbookUri={}", request.getTextbookUri());
        List<ChapterTreeNode> chapters = kgNavigationAppService.getChaptersByTextbook(request.getTextbookUri());
        return ApiResponse.success(chapters);
    }

    /**
     * 获取章节下的小节列表
     * POST /api/kg/chapters/sections
     *
     * @param request 章节 URI，格式示例：
     *   {"chapterUri": "http://edukg.org/knowledge/3.1/chapter/math#renjiao-g1s-2"}
     *   其中 renjiao-g1s-2 表示人教版一年级上册第2章
     */
    @PostMapping("/chapters/sections")
    public ApiResponse<List<SectionNode>> getSectionsByChapter(
            @RequestBody ChapterUriRequest request) {
        log.info("获取章节小节列表: chapterUri={}", request.getChapterUri());
        List<SectionNode> sections = kgNavigationAppService.getSectionsByChapter(request.getChapterUri());
        return ApiResponse.success(sections);
    }

    /**
     * 5.7 小节知识点列表
     * POST /api/kg/sections/points
     *
     * @param request 小节 URI，格式示例：
     *   {"sectionUri": "http://edukg.org/knowledge/3.1/section/math#renjiao-g1s-2-2"}
     *   其中 renjiao-g1s-2-2 表示人教版一年级上册第2章第2小节
     */
    @PostMapping("/sections/points")
    public ApiResponse<List<KgKnowledgePointDetailDTO>> getKnowledgePointsBySection(
            @RequestBody SectionUriRequest request) {
        log.info("获取小节知识点: sectionUri={}", request.getSectionUri());
        List<KgKnowledgePointDetailDTO> points = kgNavigationAppService.getKnowledgePointsBySection(request.getSectionUri());
        return ApiResponse.success(points);
    }

    /**
     * 5.8 知识点详情（含 2 层父级）
     * POST /api/kg/knowledge-points/detail
     *
     * @param request 知识点 URI，格式示例：
     *   {"kpUri": "http://edukg.org/knowledge/3.1/kp/math#..."}
     */
    @PostMapping("/knowledge-points/detail")
    public ApiResponse<KgKnowledgePointDetailDTO> getKnowledgePointDetail(
            @RequestBody KnowledgePointUriRequest request) {
        log.info("获取知识点详情: kpUri={}", request.getKpUri());
        KgKnowledgePointDetailDTO detail = kgNavigationAppService.getKnowledgePointDetail(request.getKpUri());
        return ApiResponse.success(detail);
    }

    /**
     * 知识点图谱（用于前端图谱可视化）
     * POST /api/kg/knowledge-points/graph
     *
     * @param request 知识点 URI，格式示例：
     *   {"kpUri": "http://edukg.org/knowledge/3.1/kp/math#..."}
     */
    @PostMapping("/knowledge-points/graph")
    public ApiResponse<KgGraphDTO> getKnowledgePointGraph(@RequestBody KnowledgePointUriRequest request) {
        log.info("获取知识点图谱: kpUri={}", request.getKpUri());
        KgGraphDTO graph = kgNeo4jService.getKnowledgePointGraph(request.getKpUri());
        return ApiResponse.success(graph);
    }

    // ==================== 维度配置（下拉选择器） ====================

    @GetMapping("/dimensions/textbooks")
    public ApiResponse<List<KgDimensionDTO>> getTextbooks() {
        log.info("获取教材下拉列表");
        List<KgDimensionDTO> textbooks = kgNavigationAppService.getTextbooks();
        return ApiResponse.success(textbooks);
    }

    /**
     * 获取学科列表（枚举，前端下拉用）
     * GET /api/kg/dimensions/subjects
     */
    @GetMapping("/dimensions/subjects")
    public ApiResponse<List<KgDimensionDTO>> getSubjects() {
        log.info("获取学科下拉列表");
        List<KgDimensionDTO> subjects = kgNavigationAppService.getSubjects();
        return ApiResponse.success(subjects);
    }

    /**
     * 获取年级+教材URI列表（MySQL 查询，前端下拉用）
     * POST /api/kg/dimensions/grades
     */
    @PostMapping("/dimensions/grades")
    public ApiResponse<List<GradeTextbookDTO>> getGrades(@RequestBody(required = false) GradeQueryRequest request) {
        if (request == null) {
            request = GradeQueryRequest.builder().build();
        }
        log.info("获取年级+教材列表: edition={}, subject={}", request.getEdition(), request.getSubject());
        List<GradeTextbookDTO> grades = kgNavigationAppService.getGradeTextbooks(request.getEdition(), request.getSubject());
        return ApiResponse.success(grades);
    }

    /**
     * 获取学段列表（枚举，前端下拉用）
     * GET /api/kg/dimensions/stages
     */
    @GetMapping("/dimensions/stages")
    public ApiResponse<List<KgDimensionDTO>> getStages() {
        log.info("获取学段下拉列表");
        List<KgDimensionDTO> stages = kgNavigationAppService.getStages();
        return ApiResponse.success(stages);
    }

    /**
     * 获取学科下的年级列表（支持教材版本筛选）
     * POST /api/kg/subjects/grades
     */
    @PostMapping("/subjects/grades")
    public ApiResponse<List<String>> getGradesBySubject(@RequestBody GradeQueryRequest request) {
        if (request == null) {
            request = GradeQueryRequest.builder().build();
        }
        log.info("获取学科下的年级: edition={}, subject={}", request.getEdition(), request.getSubject());
        List<String> grades = kgNavigationAppService.getGradesByEditionSubject(request.getEdition(), request.getSubject());
        return ApiResponse.success(grades);
    }

    /**
     * 获取年级下的教材列表
     * POST /api/kg/grades/textbooks
     */
    @PostMapping("/grades/textbooks")
    public ApiResponse<List<KgTextbookDTO>> getTextbooksByGrade(@RequestBody GradeRequest request) {
        log.info("获取年级下的教材: grade={}", request.getGrade());
        List<KgTextbookDTO> textbooks = kgNavigationAppService.getTextbooksByGrade(request.getGrade());
        return ApiResponse.success(textbooks);
    }

    // ==================== 年级知识体系 ====================

    /**
     * 5.9 年级知识体系
     * POST /api/kg/system/grade
     */
    @PostMapping("/system/grade")
    public ApiResponse<KgGradeSystemDTO> getGradeSystem(@RequestBody GradeSystemRequest request) {
        if (request == null) {
            request = GradeSystemRequest.builder().groupBy("subject").build();
        }
        String groupBy = request.getGroupBy() != null ? request.getGroupBy() : "subject";
        log.info("获取年级知识体系: grade={}, groupBy={}", request.getGrade(), groupBy);
        KgGradeSystemDTO system = kgKnowledgeSystemAppService.getGradeSystem(request.getGrade(), groupBy);
        return ApiResponse.success(system);
    }

    /**
     * 5.10 年级统计
     * POST /api/kg/system/stats
     */
    @PostMapping("/system/stats")
    public ApiResponse<StatsDTO> getGradeStats(@RequestBody GradeRequest request) {
        log.info("获取年级统计: grade={}", request.getGrade());
        StatsDTO stats = kgKnowledgeSystemAppService.getGradeStats(request.getGrade());
        return ApiResponse.success(stats);
    }

    // ==================== Neo4j 相关 ====================

    /**
     * 5.11 Neo4j 健康检查
     * GET /api/kg/neo4j/health
     */
    @GetMapping("/neo4j/health")
    public ApiResponse<HealthDTO> getNeo4jHealth() {
        log.info("Neo4j 健康检查");
        HealthDTO health = kgNeo4jService.getNeo4jHealth();
        return ApiResponse.success(health);
    }

    /**
     * todo 待确认
     * 5.12 批量获取概念关联
     * POST /api/kg/concepts/batch-relations
     */
    @PostMapping("/concepts/batch-relations")
    public ApiResponse<BatchRelationsDTO> batchGetConceptRelations(
            @Valid @RequestBody BatchRelationsRequest request) {
        log.info("批量获取概念关联: uris={}", request.getUris());
        BatchRelationsDTO result = kgNeo4jService.batchGetConceptRelations(request.getUris());
        return ApiResponse.success(result);
    }

    /**
     * 批量关联请求体
     */
    @lombok.Data
    public static class BatchRelationsRequest {
        private List<String> uris;
    }
}
