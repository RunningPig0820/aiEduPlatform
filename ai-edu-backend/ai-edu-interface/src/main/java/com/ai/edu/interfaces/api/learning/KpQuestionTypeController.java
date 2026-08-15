package com.ai.edu.interfaces.api.learning;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.learning.PageDTO;
import com.ai.edu.application.dto.learning.QuestionTypeKpDTO;
import com.ai.edu.application.dto.learning.QuestionTypePageItemDTO;
import com.ai.edu.application.service.learning.KpQuestionTypeQueryAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 题型库查询接口（题型分析页）。
 */
@RestController
@RequestMapping("/api/kp/question-types")
@Tag(name = "题型库查询", description = "题型库分页 + 题型关联知识点")
public class KpQuestionTypeController {

    private static final int MAX_SIZE = 100;

    @Resource
    private KpQuestionTypeQueryAppService appService;

    /** GET /api/kp/question-types — 分页列题型（CANDIDATE/STABLE）。 */
    @Operation(summary = "分页列题型", description = "题型库浏览（CANDIDATE/STABLE），分页返回")
    @GetMapping
    public ApiResponse<PageDTO<QuestionTypePageItemDTO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        TutoringAuth.requireStudent(session);
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 20;
        }
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
        return ApiResponse.success(appService.page(page, size));
    }

    /** GET /api/kp/question-types/{id}/knowledge-points — 题型关联知识点。 */
    @Operation(summary = "题型关联知识点", description = "通过题型查看关联知识点分布")
    @GetMapping("/{id}/knowledge-points")
    public ApiResponse<List<QuestionTypeKpDTO>> listKnowledgePoints(@PathVariable Long id, HttpSession session) {
        TutoringAuth.requireStudent(session);
        return ApiResponse.success(appService.listKnowledgePoints(id));
    }
}
