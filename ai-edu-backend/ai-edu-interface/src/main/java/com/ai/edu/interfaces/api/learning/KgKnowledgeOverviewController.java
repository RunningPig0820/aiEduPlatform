package com.ai.edu.interfaces.api.learning;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.learning.KgTreeNodeDTO;
import com.ai.edu.application.service.learning.KgKnowledgeOverviewAppService;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识点总览接口（知识地图底图，点击式下钻：学段→年级→课本→章节→小节→知识点）。
 *
 * <p>每次单层查询（单表或 2 表 JOIN，索引命中），替代原 7 表 JOIN 分页端点（慢 SQL）。
 * 登录即可（学生/教师/管理员），无 body，GET 路径参数下钻。
 */
@RestController
@RequestMapping("/api/kg")
@Tag(name = "知识点总览", description = "知识地图底图点击式下钻（学段→年级→课本→章节→小节→知识点，每次单层查询）")
public class KgKnowledgeOverviewController {

    @Resource
    private KgKnowledgeOverviewAppService appService;

    /**
     * GET /api/kg/grades?stage=primary — 学段下年级列表（知识地图第 1 层）。
     *
     * <p>⚠️ 讨巧实现：当前年级直接由教材表 {@code t_kg_textbook} 的 {@code DISTINCT grade} 推导
     * （数据现状仅人教版 + 数学，教材表年级够用）。**正确方向**：从组织域取「学段→年级」权威关系
     * （{@code com.ai.edu.domain.organization} 的 {@code SchoolStageEnum} + {@code GradeEnum} / Grade 实体），
     * 待教材覆盖多版本/多学科或组织域年级数据就绪后切换，避免教材表年级不完整时漏年级。
     */
    @Operation(summary = "学段下年级列表", description = "第 1 层：按学段查年级（stage: primary/middle/high；当前由教材表推导，正确应从组织域取年级权威关系）")
    @GetMapping("/grades")
    public ApiResponse<List<KgTreeNodeDTO>> grades(@RequestParam String stage, HttpSession session) {
        requireLogin(session);
        return ApiResponse.success(appService.gradesByStage(stage));
    }

    /** GET /api/kg/textbooks?stage=primary&grade=一年级 — 年级下课本列表（第 2 层）。 */
    @Operation(summary = "年级下课本列表", description = "第 2 层：按学段+年级查课本")
    @GetMapping("/textbooks")
    public ApiResponse<List<KgTreeNodeDTO>> textbooks(@RequestParam String stage,
                                                      @RequestParam String grade,
                                                      HttpSession session) {
        requireLogin(session);
        return ApiResponse.success(appService.textbooksByStage(stage, grade));
    }

    /** GET /api/kg/textbooks/chapters?textbookUri=xxx&stage=primary — 课本下章节列表（第 3 层）。
     *  uri 含 http://、# 等特殊字符，放 path segment 会被 Tomcat 拒 400——必须 query 传参；
     *  stage 为学段上下文（前端传入，当前章节查询按 textbookUri，不参与过滤）。 */
    @Operation(summary = "课本下章节列表", description = "第 3 层：按课本查章节（textbookUri 须 encodeURIComponent；stage 学段上下文可选）")
    @GetMapping("/textbooks/chapters")
    public ApiResponse<List<KgTreeNodeDTO>> chapters(@RequestParam String textbookUri,
                                                     @RequestParam(required = false) String stage,
                                                     HttpSession session) {
        requireLogin(session);
        return ApiResponse.success(appService.chaptersByTextbook(textbookUri));
    }

    /** GET /api/kg/chapters/sections?chapterUri=xxx — 章节下小节列表（第 4 层，query 传参防 path 400）。 */
    @Operation(summary = "章节下小节列表", description = "第 4 层：按章节查小节（chapterUri 须 encodeURIComponent）")
    @GetMapping("/chapters/sections")
    public ApiResponse<List<KgTreeNodeDTO>> sections(@RequestParam String chapterUri, HttpSession session) {
        requireLogin(session);
        return ApiResponse.success(appService.sectionsByChapter(chapterUri));
    }

    /** GET /api/kg/sections/knowledge-points?sectionUri=xxx — 小节下知识点列表（第 5 层，query 传参防 path 400）。 */
    @Operation(summary = "小节下知识点列表", description = "第 5 层：按小节查知识点（sectionUri 须 encodeURIComponent）")
    @GetMapping("/sections/knowledge-points")
    public ApiResponse<List<KgTreeNodeDTO>> knowledgePoints(@RequestParam String sectionUri, HttpSession session) {
        requireLogin(session);
        return ApiResponse.success(appService.knowledgePointsBySection(sectionUri));
    }

    private void requireLogin(HttpSession session) {
        if (TutoringAuth.currentStudentId(session) == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
    }
}
