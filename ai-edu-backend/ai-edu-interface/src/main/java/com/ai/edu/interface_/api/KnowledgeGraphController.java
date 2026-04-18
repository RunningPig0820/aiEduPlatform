package com.ai.edu.interface_.api;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.kg.*;
import com.ai.edu.application.service.KgKnowledgeSystemAppService;
import com.ai.edu.application.service.KgNavigationAppService;
import com.ai.edu.application.service.KgNeo4jService;
import com.ai.edu.application.service.KgSyncAppService;
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
     * 5.4 同步历史记录
     * GET /api/kg/sync/records
     */
    @GetMapping("/sync/records")
    public ApiResponse<List<SyncRecordDTO>> getSyncRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("查询同步历史: page={}, size={}", page, size);
        List<SyncRecordDTO> records = kgSyncAppService.getSyncRecords(page, size);
        return ApiResponse.success(records);
    }

    // ==================== 教材/章节导航 ====================

    /**
     * 5.5 教材列表
     * GET /api/kg/textbooks
     */
    @GetMapping("/textbooks")
    public ApiResponse<List<KgTextbookDTO>> getTextbooks(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String stage) {
        log.info("获取教材列表: subject={}, stage={}", subject, stage);
        List<KgTextbookDTO> textbooks = kgNavigationAppService.getTextbooks(subject, stage);
        return ApiResponse.success(textbooks);
    }

    /**
     * 5.6 教材章节树
     * GET /api/kg/textbooks/{uri}/chapters
     * 注意: {uri} 需要 URL 编码
     */
    @GetMapping("/textbooks/{uri}/chapters")
    public ApiResponse<List<ChapterTreeNode>> getChaptersByTextbook(
            @PathVariable String uri) {
        log.info("获取教材章节树: textbookUri={}", uri);
        List<ChapterTreeNode> chapters = kgNavigationAppService.getChaptersByTextbook(uri);
        return ApiResponse.success(chapters);
    }

    /**
     * 5.7 小节知识点列表
     * GET /api/kg/sections/{uri}/points
     */
    @GetMapping("/sections/{uri}/points")
    public ApiResponse<List<KgKnowledgePointDetailDTO>> getKnowledgePointsBySection(
            @PathVariable String uri) {
        log.info("获取小节知识点: sectionUri={}", uri);
        List<KgKnowledgePointDetailDTO> points = kgNavigationAppService.getKnowledgePointsBySection(uri);
        return ApiResponse.success(points);
    }

    /**
     * 5.8 知识点详情（含 2 层父级）
     * GET /api/kg/knowledge-points/{uri}
     */
    @GetMapping("/knowledge-points/{uri}")
    public ApiResponse<KgKnowledgePointDetailDTO> getKnowledgePointDetail(
            @PathVariable String uri) {
        log.info("获取知识点详情: kpUri={}", uri);
        KgKnowledgePointDetailDTO detail = kgNavigationAppService.getKnowledgePointDetail(uri);
        return ApiResponse.success(detail);
    }

    /**
     * 知识点图谱（用于前端图谱可视化）
     * GET /api/kg/knowledge-points/{uri}/graph
     */
    @GetMapping("/knowledge-points/{uri}/graph")
    public ApiResponse<KgGraphDTO> getKnowledgePointGraph(@PathVariable String uri) {
        log.info("获取知识点图谱: kpUri={}", uri);
        KgGraphDTO graph = kgNeo4jService.getKnowledgePointGraph(uri);
        return ApiResponse.success(graph);
    }

    // ==================== 维度配置（下拉选择器） ====================

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
     * 获取年级列表（MySQL 聚合，前端下拉用）
     * GET /api/kg/dimensions/grades
     */
    @GetMapping("/dimensions/grades")
    public ApiResponse<List<String>> getGrades() {
        log.info("获取年级下拉列表");
        List<String> grades = kgNavigationAppService.getGrades();
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
     * 获取教材列表（枚举，前端下拉用）
     * GET /api/kg/dimensions/textbooks
     */
    @GetMapping("/dimensions/textbooks")
    public ApiResponse<List<KgDimensionDTO>> getTextbooks() {
        log.info("获取教材下拉列表");
        List<KgDimensionDTO> textbooks = kgNavigationAppService.getTextbooks();
        return ApiResponse.success(textbooks);
    }

    /**
     * 获取学科下的年级列表
     * GET /api/kg/subjects/{subject}/grades
     */
    @GetMapping("/subjects/{subject}/grades")
    public ApiResponse<List<String>> getGradesBySubject(@PathVariable String subject) {
        log.info("获取学科下的年级: subject={}", subject);
        List<String> grades = kgNavigationAppService.getGradesBySubject(subject);
        return ApiResponse.success(grades);
    }

    /**
     * 获取年级下的教材列表
     * GET /api/kg/grades/{grade}/textbooks
     */
    @GetMapping("/grades/{grade}/textbooks")
    public ApiResponse<List<KgTextbookDTO>> getTextbooksByGrade(@PathVariable String grade) {
        log.info("获取年级下的教材: grade={}", grade);
        List<KgTextbookDTO> textbooks = kgNavigationAppService.getTextbooksByGrade(grade);
        return ApiResponse.success(textbooks);
    }

    // ==================== 年级知识体系 ====================

    /**
     * 5.9 年级知识体系
     * GET /api/kg/system/grade/{grade}
     */
    @GetMapping("/system/grade/{grade}")
    public ApiResponse<KgGradeSystemDTO> getGradeSystem(
            @PathVariable String grade,
            @RequestParam(required = false, defaultValue = "subject") String groupBy) {
        log.info("获取年级知识体系: grade={}, groupBy={}", grade, groupBy);
        KgGradeSystemDTO system = kgKnowledgeSystemAppService.getGradeSystem(grade, groupBy);
        return ApiResponse.success(system);
    }

    /**
     * 5.10 年级统计
     * GET /api/kg/system/stats/{grade}
     */
    @GetMapping("/system/stats/{grade}")
    public ApiResponse<StatsDTO> getGradeStats(@PathVariable String grade) {
        log.info("获取年级统计: grade={}", grade);
        StatsDTO stats = kgKnowledgeSystemAppService.getGradeStats(grade);
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
