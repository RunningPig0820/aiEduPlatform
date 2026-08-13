package com.ai.edu.interfaces.api.learning;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.learning.TutoringConfigDTO;
import com.ai.edu.application.dto.learning.TutoringSessionDTO;
import com.ai.edu.application.dto.learning.TutoringSessionListItemDTO;
import com.ai.edu.application.dto.learning.TutoringTranscriptDTO;
import com.ai.edu.application.dto.learning.command.SendMessageCommand;
import com.ai.edu.application.dto.learning.command.StartTutoringCommand;
import com.ai.edu.application.service.learning.TutoringAppService;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.learning.model.contract.OcrResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;

/**
 * AI 答疑 REST API（SSE 类型先行流式 + 同步端点）。
 *
 * <p>认证：studentId 从 HttpSession 取并校验 STUDENT 角色，请求体不传身份，服务端不信任客户端。
 * SSE 端点未登录/越权返回 {@code event: error}（不开流）；同步端点抛业务异常 → GlobalExceptionHandler。
 *
 * <p>拓扑：前端 → 本网关（认证/护栏/落库/COS）→ Python 答疑 agent（decide/generate，内部 token）。
 * <b>不暴露</b> Python 内部端点（decide/generate/ocr-recognize），仅内部可达。
 */
@Slf4j
@RestController
@RequestMapping("/api/tutoring")
@Tag(name = "AI 答疑", description = "答疑会话（SSE 类型先行流式）/断点恢复/收尾/拍题OCR/掌握度")
public class TutoringController {

    @Resource
    private TutoringAppService tutoringAppService;

    /** 发起答疑会话（SSE：meta → token → done，文字题）。 */
    @Operation(summary = "发起答疑会话", description = "首条消息进答疑，SSE 类型先行流式返回引导")
    @PostMapping(value = "/sessions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> start(@Valid @RequestBody StartTutoringCommand command, HttpSession session) {
        Flux<ServerSentEvent<String>> authError = authError(session);
        if (authError != null) {
            return authError;
        }
        return tutoringAppService.start(TutoringAuth.currentStudentId(session), command.getMessage());
    }

    /** 发起答疑会话（SSE，图片题，multipart）：上传题目照片直接建会话，图片作为首条消息进答疑。 */
    @Operation(summary = "发起答疑会话（图片）", description = "multipart 上传题目照片，图片作为首条消息进答疑，SSE 流式返回引导")
    @PostMapping(value = "/sessions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> startWithImage(@RequestParam(value = "content", required = false) String content,
                                                        @RequestParam("file") MultipartFile file, HttpSession session) {
        Flux<ServerSentEvent<String>> authError = authError(session);
        if (authError != null) {
            return authError;
        }
        try {
            return tutoringAppService.start(TutoringAuth.currentStudentId(session), content,
                    file.getBytes(), file.getOriginalFilename());
        } catch (IOException e) {
            log.warn("读取题目图片失败: {}", e.getMessage());
            return sseError(ErrorCode.INVALID_PARAMS, "读取图片失败");
        }
    }

    /** 发送学生回答（SSE）。sessionId 以路径为准，body 只取 content。 */
    @Operation(summary = "发送学生回答", description = "触发 decide→护栏→generate，SSE 流式返回")
    @PostMapping(value = "/sessions/{sessionId}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> sendMessage(@PathVariable Long sessionId,
                                                     @RequestBody SendMessageCommand command, HttpSession session) {
        Flux<ServerSentEvent<String>> authError = authError(session);
        if (authError != null) {
            return authError;
        }
        if (command == null || command.getContent() == null || command.getContent().isBlank()) {
            return sseError(ErrorCode.INVALID_PARAMS, "消息不能为空");
        }
        return tutoringAppService.sendMessage(TutoringAuth.currentStudentId(session), sessionId, command.getContent());
    }

    /**
     * 发送学生消息（SSE，图片，multipart）：上传新题目图片 = 换题信号 → decide 带 is_new_question=true。
     * Java 收到新图、检测新 URL 未在 history 中出现，该轮 Python 直接返回 switch，Java 重置轮次计数。
     */
    @Operation(summary = "发送学生消息（图片/换题）", description = "multipart 上传新题目图片（换题）或配图消息，SSE 流式返回")
    @PostMapping(value = "/sessions/{sessionId}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> sendImageMessage(@PathVariable Long sessionId,
                                                          @RequestParam(value = "content", required = false) String content,
                                                          @RequestParam("file") MultipartFile file, HttpSession session) {
        Flux<ServerSentEvent<String>> authError = authError(session);
        if (authError != null) {
            return authError;
        }
        try {
            return tutoringAppService.sendMessage(TutoringAuth.currentStudentId(session), sessionId, content,
                    file.getBytes(), file.getOriginalFilename());
        } catch (IOException e) {
            log.warn("读取图片失败: {}", e.getMessage());
            return sseError(ErrorCode.INVALID_PARAMS, "读取图片失败");
        }
    }

    /** 请求答案（SSE，走答案护栏：第 1 次思路 / 第 2 次答案）。 */
    @Operation(summary = "请求答案", description = "走答案护栏，SSE 流式返回思路或完整答案")
    @PostMapping(value = "/sessions/{sessionId}/request-answer", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> requestAnswer(@PathVariable Long sessionId, HttpSession session) {
        Flux<ServerSentEvent<String>> authError = authError(session);
        if (authError != null) {
            return authError;
        }
        return tutoringAppService.requestAnswer(TutoringAuth.currentStudentId(session), sessionId);
    }

    /** 前端能力配置（ocr.enabled → 前端据此显示/隐藏拍照入口）。 */
    @Operation(summary = "答疑能力配置", description = "返回 OCR 开关等前端能力配置")
    @GetMapping("/config")
    public ApiResponse<TutoringConfigDTO> getConfig(HttpSession session) {
        TutoringAuth.requireStudent(session);
        return ApiResponse.success(tutoringAppService.getTutoringConfig());
    }

    /** 会话历史列表（全状态，updated_at 倒序，不含软删）。 */
    @Operation(summary = "会话历史列表", description = "当前学生全部会话（含已归档/终止），updated_at 倒序；内容经 GET /sessions/{id}/transcript 后端代理拉取")
    @GetMapping("/sessions")
    public ApiResponse<List<TutoringSessionListItemDTO>> listSessions(HttpSession session) {
        Long studentId = TutoringAuth.requireStudent(session);
        return ApiResponse.success(tutoringAppService.listSessions(studentId));
    }

    /** 删除会话（软删 + 清 Redis 缓存；COS 保留可恢复）。 */
    @Operation(summary = "删除会话", description = "软删会话并清 Redis 缓存；归属校验失败返回 50002（不泄露存在性）")
    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> deleteSession(@PathVariable Long sessionId, HttpSession session) {
        Long studentId = TutoringAuth.requireStudent(session);
        tutoringAppService.deleteSession(studentId, sessionId);
        return ApiResponse.success(null);
    }

    /** 查询会话状态（断点恢复）。 */
    @Operation(summary = "查询会话状态", description = "返回会话状态/计数/最近消息，供中断后恢复续聊")
    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<TutoringSessionDTO> getSession(@PathVariable Long sessionId, HttpSession session) {
        Long studentId = TutoringAuth.requireStudent(session);
        return ApiResponse.success(tutoringAppService.getSession(studentId, sessionId));
    }

    /** 获取会话完整 transcript（后端代理读 COS，前端零 COS 直连）。 */
    @Operation(summary = "获取会话 transcript", description = "后端服务端读 COS 透传消息数组（含 meta）；归属校验失败 50002，COS 对象缺失返回空 messages（前端兜底 recentMessages）")
    @GetMapping("/sessions/{sessionId}/transcript")
    public ApiResponse<TutoringTranscriptDTO> getTranscript(@PathVariable Long sessionId, HttpSession session) {
        Long studentId = TutoringAuth.requireStudent(session);
        return ApiResponse.success(TutoringTranscriptDTO.builder()
                .messages(tutoringAppService.getTranscript(studentId, sessionId))
                .build());
    }

    /** 主动结束并归档会话（end_reason=ABANDONED，掌握度不提升 + COS 终态写）。 */
    @Operation(summary = "结束并归档会话", description = "学生主动收尾，置 ARCHIVED 并写 COS transcript 终态")
    @PostMapping("/sessions/{sessionId}/archive")
    public ApiResponse<TutoringSessionDTO> archive(@PathVariable Long sessionId, HttpSession session) {
        Long studentId = TutoringAuth.requireStudent(session);
        return ApiResponse.success(tutoringAppService.archive(studentId, sessionId));
    }

    /** 拍题识别（OCR 前置步骤，multipart）：识别文本经学生确认/修改后作为首条消息进答疑。 */
    @Operation(summary = "拍题识别OCR", description = "上传题目照片，识别为文本供确认后进答疑")
    @PostMapping("/ocr")
    public ApiResponse<OcrResult> ocr(@RequestParam("file") MultipartFile file, HttpSession session) {
        TutoringAuth.requireStudent(session);
        try {
            return ApiResponse.success(tutoringAppService.ocr(file.getBytes(), file.getOriginalFilename()));
        } catch (IOException e) {
            log.warn("读取 OCR 图片失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.TUTORING_OCR_INVALID, "读取图片失败");
        }
    }

    // ==================== helpers ====================

    /** SSE 端点认证预检：未登录/非 STUDENT 返回 error 事件（不开流），通过返回 null。 */
    private Flux<ServerSentEvent<String>> authError(HttpSession session) {
        if (TutoringAuth.currentStudentId(session) == null) {
            return sseError(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!TutoringAuth.isStudent(session)) {
            return sseError(ErrorCode.PERMISSION_DENIED, "仅学生可访问");
        }
        return null;
    }

    /** 未登录/越权的 SSE error 事件（不开流，前端按 error 事件处理）。 */
    private Flux<ServerSentEvent<String>> sseError(String code, String message) {
        return Flux.just(ServerSentEvent.<String>builder()
                .event("error")
                .data("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}")
                .build());
    }
}
