package com.ai.edu.interfaces.api.learning;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.learning.KpResolveDTO;
import com.ai.edu.application.dto.learning.KpResolveRequest;
import com.ai.edu.application.dto.learning.QuestionAnalysisDTO;
import com.ai.edu.application.service.learning.KpAppService;
import com.ai.edu.application.service.learning.KpQuestionAnalysisAppService;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 题型解析接口（POST /api/kp/resolve）——将 AI 识别的题型/知识点 label 解析到教材知识点 URI。
 */
@RestController
@RequestMapping("/api/kp")
@Tag(name = "知识点解析", description = "题型/知识点 label → TextbookKP URI 解析")
public class KpResolutionController {

    @Resource
    private KpAppService kpAppService;
    @Resource
    private KpQuestionAnalysisAppService kpQuestionAnalysisAppService;

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

    /**
     * POST /api/kp/aggregation/run — 已停用（域 B 独立化 Decision 10）：
     * obs 共现自动关联不再使用，改由 POST /api/kp/type/upsert 维护题型↔知识点（tasks 2.0.5/2.0.6）。
     * 接口保留返回提示，避免历史调用方误以为聚合已执行。
     */
    @Operation(summary = "题型库聚合（已停用）", description = "域 B 独立化后 obs 共现自动关联停用，请用 POST /api/kp/type/upsert 维护")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/aggregation/run")
    public ApiResponse<String> runAggregation() {
        return ApiResponse.success("已停用：域 B 独立化后 obs 共现自动关联不再使用，请用 POST /api/kp/type/upsert 维护题型↔知识点");
    }

    /** POST /api/kp/analyze-question/image — 图片题目分析：multipart 图片 → 视觉模型直接看图 → 题型+知识点（不经 OCR）。 */
    @Operation(summary = "图片题目分析", description = "图片直接走多模态视觉模型看图（OCR 仅前端失败兜底）")
    @PostMapping(value = "/analyze-question/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<QuestionAnalysisDTO> analyzeQuestionImage(@RequestParam("file") MultipartFile file,
                                                                 HttpSession session) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "file 不能为空");
        }
        Long studentId = TutoringAuth.requireStudent(session);
        try {
            return ApiResponse.success(kpQuestionAnalysisAppService
                    .analyzeImage(file.getBytes(), file.getOriginalFilename(), studentId));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "图片读取失败");
        }
    }

    /** POST /api/kp/analyze-question — 单题分析：题目文本 → 题型名 → 关联知识点清单（纯分析不写观测）。 */
    @Operation(summary = "单题分析", description = "贴题/拍题（OCR 后）→ 识别题型 → 关联知识点清单；PENDING 不报错携带澄清候选")
    @PostMapping("/analyze-question")
    public ApiResponse<QuestionAnalysisDTO> analyzeQuestion(@RequestBody AnalyzeQuestionRequest request, HttpSession session) {
        if (request == null || request.getText() == null || request.getText().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "text 不能为空");
        }
        Long studentId = TutoringAuth.requireStudent(session);
        return ApiResponse.success(kpQuestionAnalysisAppService.analyze(request.getText(), studentId));
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

    /** 单题分析请求体。 */
    @Data
    public static class AnalyzeQuestionRequest {
        private String text;
    }
}
