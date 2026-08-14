package com.ai.edu.interfaces.api.learning;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.learning.KpResolveDTO;
import com.ai.edu.application.dto.learning.KpResolveRequest;
import com.ai.edu.application.service.learning.KpAppService;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.Data;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题型解析接口（POST /api/kp/resolve）——将 AI 识别的题型/知识点 label 解析到教材知识点 URI。
 */
@RestController
@RequestMapping("/api/kp")
@Tag(name = "知识点解析", description = "题型/知识点 label → TextbookKP URI 解析")
public class KpResolutionController {

    @Resource
    private KpAppService kpAppService;

    /** POST /api/kp/resolve — 复用答疑内嵌解析管线（镜像 → 题型库年级匹配 → LLM 消歧 → 挂起）。 */
    @Operation(summary = "题型解析", description = "低置信返回 status=PENDING（不报错），携带候选供学生澄清")
    @PostMapping("/resolve")
    public ApiResponse<KpResolveDTO> resolve(@RequestBody KpResolveRequest request, HttpSession session) {
        if (request == null || request.getLabel() == null || request.getLabel().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "label 不能为空");
        }
        Long studentId = TutoringAuth.currentStudentId(session);
        return ApiResponse.success(kpAppService.resolve(request.getLabel(), studentId));
    }

    /** POST /api/kp/vote — 学生澄清投票（选"你想学哪个"），落 source=student_vote 观测。 */
    @Operation(summary = "学生澄清投票", description = "学生从澄清候选中选择归属概念，落 student_vote 观测")
    @PostMapping("/vote")
    public ApiResponse<Void> vote(@RequestBody VoteRequest request, HttpSession session) {
        Long studentId = TutoringAuth.requireStudent(session);
        if (request == null || request.getTopicLabel() == null || request.getTopicLabel().isBlank()
                || request.getSelectedLabel() == null || request.getSelectedLabel().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "topicLabel/selectedLabel 不能为空");
        }
        kpAppService.vote(request.getTopicLabel(), studentId, request.getSelectedLabel());
        return ApiResponse.success();
    }

    /** 澄清投票请求体。 */
    @Data
    public static class VoteRequest {
        private String topicLabel;
        private String selectedLabel;
    }
}
