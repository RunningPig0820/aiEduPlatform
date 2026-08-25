package com.ai.edu.interfaces.api.learning;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.learning.command.RagAskCommand;
import com.ai.edu.application.service.learning.RagAssistantAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * RAG 项目介绍助手 REST API（SSE 白盒流式 + 非流式两模式）。
 *
 * <p>认证：角色从可信 session 取（STUDENT 才放行），非学生/缺失 → 固定 403 响应体，不进入 RAG 流程、
 * 不调 LLM、不产生 trace。body 传 role 一律忽略（本控制器只读 session，不信任前端传参）。
 * 前端 → Java 网关为 camelCase；Java 桥 → Python 为 snake_case（桥内转换）。
 */
@Slf4j
@RestController
@RequestMapping("/api/rag/assistant")
@Tag(name = "RAG 项目介绍助手", description = "白盒 RAG 问答（SSE 流式 + 非流式）/角色门/关闭对话/断线补查/评估报告")
public class RagAssistantController {

    @Resource
    private RagAssistantAppService ragAssistantAppService;

    /** 发起一轮问答：SSE 白盒事件流（permission → intent → ... → done），或非流式 done 摘要。 */
    @Operation(summary = "发起问答", description = "SSE 白盒事件流（角色门仅 STUDENT，非学生固定 403）")
    @PostMapping(value = "/ask")
    public ResponseEntity<?> ask(@Valid @RequestBody RagAskCommand command, HttpSession session) {
        if (!TutoringAuth.isStudent(session)) {
            log.info("[rag-assistant] 角色门拒绝: sessionRole={}",
                    session == null ? null : session.getAttribute("role"));
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"code\":\"403\",\"message\":\"仅学生可访问此助手\"}");
        }
        if (Boolean.TRUE.equals(command.getStream())) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(ragAssistantAppService.ask(command));
        }
        return ResponseEntity.ok(ApiResponse.success(ragAssistantAppService.askStages(command)));
    }
}
