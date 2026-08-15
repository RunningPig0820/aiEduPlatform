package com.ai.edu.interfaces.api.learning;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.learning.KgKnowledgePointPageItemDTO;
import com.ai.edu.application.dto.learning.KgKnowledgePointPageRequest;
import com.ai.edu.application.dto.learning.PageDTO;
import com.ai.edu.application.service.learning.KgKnowledgeOverviewAppService;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识点总览查询接口（学生端"知识点总览"知识地图底图）。
 */
@RestController
@RequestMapping("/api/kg")
@Tag(name = "知识点总览", description = "按学段分页列全量教材知识点")
public class KgKnowledgeOverviewController {

    private static final int MAX_SIZE = 100;

    @Resource
    private KgKnowledgeOverviewAppService appService;

    /** POST /api/kg/knowledge-points — 按学段分页列知识点（登录即可，学生/教师/管理员）。 */
    @Operation(summary = "按学段分页列知识点", description = "知识地图底图（学段→章节→知识点）")
    @PostMapping("/knowledge-points")
    public ApiResponse<PageDTO<KgKnowledgePointPageItemDTO>> page(
            @RequestBody(required = false) KgKnowledgePointPageRequest request,
            HttpSession session) {
        if (TutoringAuth.currentStudentId(session) == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        String stage = request == null ? null : request.getStage();
        int page = request == null || request.getPage() == null ? 1 : request.getPage();
        int size = request == null || request.getSize() == null ? 20 : request.getSize();
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 20;
        }
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
        return ApiResponse.success(appService.page(stage, page, size));
    }
}
