package com.ai.edu.interfaces.api.learning;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.learning.command.RagAskCommand;
import com.ai.edu.application.dto.learning.rag.RagCloseDTO;
import com.ai.edu.application.dto.learning.rag.RagEvalReportDTO;
import com.ai.edu.application.dto.learning.rag.RagEvalRunDTO;
import com.ai.edu.application.dto.learning.rag.RagGuideDTO;
import com.ai.edu.application.dto.learning.rag.SseDoneDTO;
import com.ai.edu.application.service.learning.RagAssistantAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * RAG 项目介绍助手 REST API（SSE 白盒流式 + 非流式两模式）。
 *
 * <p>认证：角色从可信 session 取（STUDENT 才放行），非学生/缺失 → 固定 403（{@code ResponseStatusException}
 * 由 GlobalExceptionHandler 转 403 响应体），不进入 RAG 流程、不调 LLM、不产生 trace。body 传 role 一律忽略。
 * SSE 端点<b>直接返回 {@code Flux<ServerSentEvent<String>>}</b>（包 ResponseEntity 会丢泛型、
 * Spring MVC 找不到 converter——见 HttpMessageNotWritableException 修复）。
 * 前端 → Java 网关 camelCase；Java 桥 → Python snake_case（桥内转换）。
 */
@Slf4j
@RestController
@RequestMapping("/api/rag/assistant")
@Tag(name = "RAG 项目介绍助手", description = "白盒 RAG 问答（SSE 流式 + 非流式）/角色门/关闭对话/断线补查/评估报告")
public class RagAssistantController {

    @Resource
    private RagAssistantAppService ragAssistantAppService;

    /** 发起一轮问答（SSE 流式）：permission → intent → ... → done。非学生 → 403。 */
    @Operation(summary = "发起问答（SSE 流式）", description = "白盒事件流，角色门仅 STUDENT，非学生固定 403")
    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> ask(@Valid @RequestBody RagAskCommand command, HttpSession session) {
        requireStudent(session);
        return ragAssistantAppService.ask(command);
    }

    /** 发起一轮问答（非流式）：done 结构 + stages 摘要。非学生 → 403。 */
    @Operation(summary = "发起问答（非流式）", description = "返回 done 结构 + stages 摘要，角色门仅 STUDENT")
    @PostMapping(value = "/ask/sync")
    public ApiResponse<Map<String, Object>> askSync(@Valid @RequestBody RagAskCommand command, HttpSession session) {
        requireStudent(session);
        return ApiResponse.success(ragAssistantAppService.askStages(command));
    }

    /** 查看原文（Java 代理）：转发 Python 源文件静态服务，返回原文内容（ApiResponse 包裹，匹配前端 axios 拦截器）。非学生 → 403。 */
    @Operation(summary = "查看原文", description = "Java 网关代理 Python 源文件，前端不直连 Python；STUDENT 角色门")
    @GetMapping("/source")
    public Mono<ApiResponse<String>> source(@RequestParam("path") String path, HttpSession session) {
        requireStudent(session);
        return ragAssistantAppService.source(path).map(ApiResponse::success);
    }

    /** 评估报告（Java 代理）：转发 Python baseline 报告白盒（hit@3/质量分/成本/耗时/样本细节），snake→camel。非学生 → 403。 */
    @Operation(summary = "评估报告", description = "白盒展示 RAG baseline 评估报告（含 evaluatedAt/样本细节/running）；STUDENT 角色门；暂无报告 → 10002")
    @GetMapping("/eval/report")
    public Mono<ApiResponse<RagEvalReportDTO>> evalReport(HttpSession session) {
        requireStudent(session);
        return ragAssistantAppService.evalReport().map(ApiResponse::success);
    }

    /** 触发重评测（Java 代理，异步）：Python 后台跑一轮评估，秒回 running 状态；前端轮询 report.running 后刷新。非学生 → 403。 */
    @Operation(summary = "触发重评测", description = "异步跑一轮真实评估（几分钟）；已有一轮在跑 → 幂等 {running:true,alreadyRunning:true}；STUDENT 角色门")
    @PostMapping("/eval/run")
    public Mono<ApiResponse<RagEvalRunDTO>> evalRun(HttpSession session) {
        requireStudent(session);
        return ragAssistantAppService.evalRun().map(ApiResponse::success);
    }

    /** 开始引导（Java 代理）：转发 Python 模块引导底座池出题（1~3 条，必含 ≥1 条 RAG 方向）。非学生 → 403。 */
    @Operation(summary = "开始引导", description = "会话入口 chips，模块底座池驱动，必含 RAG 方向；STUDENT 角色门；currentProject 可选，缺省 Python 兜底")
    @GetMapping("/guide")
    public Mono<ApiResponse<RagGuideDTO>> guide(
            @RequestParam(value = "currentProject", required = false) String currentProject,
            HttpSession session) {
        requireStudent(session);
        return ragAssistantAppService.guide(currentProject).map(ApiResponse::success);
    }

    /** 关闭对话（Java 侧结算）：置 session closed + 返回会话累计 token/轮数。非学生 → 403；未知会话 → 10002。 */
    @Operation(summary = "关闭对话", description = "结束会话 + 结算累计 token/轮数；幂等（已关闭也返回 true）；STUDENT 角色门；未知会话 → 10002")
    @PostMapping("/sessions/{sessionId}/close")
    public Mono<ApiResponse<RagCloseDTO>> close(@PathVariable String sessionId, HttpSession session) {
        requireStudent(session);
        return ragAssistantAppService.close(sessionId).map(ApiResponse::success);
    }

    /** 断线补查单轮结果：读 Java Redis trace 快照。非学生 → 403；trace 不存在 → 10002。 */
    @Operation(summary = "断线补查", description = "凭 traceId 找回该轮完整回答（answer/quotedKeys/tokensUsage/suggestions）；STUDENT 角色门；不存在 → 10002")
    @GetMapping("/turns/{traceId}")
    public Mono<ApiResponse<SseDoneDTO>> turn(@PathVariable String traceId, HttpSession session) {
        requireStudent(session);
        return ragAssistantAppService.turn(traceId).map(ApiResponse::success);
    }

    private void requireStudent(HttpSession session) {
        if (!TutoringAuth.isStudent(session)) {
            log.info("[rag-assistant] 角色门拒绝: sessionRole={}",
                    session == null ? null : session.getAttribute("role"));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅学生可访问此助手");
        }
    }
}
