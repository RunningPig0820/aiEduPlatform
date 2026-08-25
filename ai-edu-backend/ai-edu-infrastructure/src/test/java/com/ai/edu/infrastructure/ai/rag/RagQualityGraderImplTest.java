package com.ai.edu.infrastructure.ai.rag;

import com.ai.edu.domain.learning.model.contract.RagQualityScore;
import com.ai.edu.domain.llm.model.AiEduChatRequest;
import com.ai.edu.domain.llm.model.AiEduChatResponse;
import com.ai.edu.domain.llm.service.LlmGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * RagQualityGraderImpl 打分单测：LLM 返回 JSON → 解析为 RagQualityScore；
 * 容错代码块/越界/LLM 异常 → 返回 empty（不入累计，不打断问答）。
 */
class RagQualityGraderImplTest {

    private RagQualityGraderImpl grader;
    private LlmGateway llmGateway;

    @BeforeEach
    void setUp() {
        llmGateway = mock(LlmGateway.class);
        grader = new RagQualityGraderImpl();
        ReflectionTestUtils.setField(grader, "llmGateway", llmGateway);
    }

    @Test
    @DisplayName("grade: LLM 返回 JSON → 解析 score/reason")
    void grade_parsesScore() {
        when(llmGateway.chat(any(AiEduChatRequest.class))).thenReturn(Mono.just(
                AiEduChatResponse.builder().response("{\"score\":4,\"reason\":\"回答相关且引用准确\"}").build()));

        RagQualityScore score = grader.grade("项目架构是什么", "整体是……", List.of("b1"), List.of()).block();
        assertEquals(4, score.getScore());
        assertEquals("回答相关且引用准确", score.getReason());
    }

    @Test
    @DisplayName("grade: 容忍 LLM 在 JSON 外套 ```json 代码块")
    void grade_toleratesCodeFence() {
        when(llmGateway.chat(any(AiEduChatRequest.class))).thenReturn(Mono.just(
                AiEduChatResponse.builder().response("```json\n{\"score\":5,\"reason\":\"很好\"}\n```").build()));

        RagQualityScore score = grader.grade("q", "a", List.of(), List.of()).block();
        assertEquals(5, score.getScore());
    }

    @Test
    @DisplayName("grade: LLM 异常 → 返回 empty（不入累计，不打断问答）")
    void grade_llmErrorReturnsEmpty() {
        when(llmGateway.chat(any(AiEduChatRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("llm down")));

        assertNull(grader.grade("q", "a", List.of(), List.of()).block());
    }

    @Test
    @DisplayName("grade: 评分越界(9) → 返回 empty（不累计脏数据）")
    void grade_outOfRangeReturnsEmpty() {
        when(llmGateway.chat(any(AiEduChatRequest.class))).thenReturn(Mono.just(
                AiEduChatResponse.builder().response("{\"score\":9,\"reason\":\"x\"}").build()));

        assertNull(grader.grade("q", "a", List.of(), List.of()).block());
    }

    @Test
    @DisplayName("grade: prompt 含维度定义/打分标尺/忠实度强规则/引用片段摘要")
    void grade_promptIncludesRubricAndSummaries() {
        AtomicReference<AiEduChatRequest> captured = new AtomicReference<>();
        when(llmGateway.chat(any(AiEduChatRequest.class))).thenAnswer(inv -> {
            captured.set(inv.getArgument(0));
            return Mono.just(AiEduChatResponse.builder().response("{\"score\":3,\"reason\":\"ok\"}").build());
        });

        grader.grade("项目架构", "整体分层……", List.of("b1"),
                List.of("【架构】分四层……", "【数据流】入库流程……")).block();

        String prompt = captured.get().getMessage();
        assertTrue(prompt.contains("忠实度"), prompt);
        assertTrue(prompt.contains("5分"), prompt);
        assertTrue(prompt.contains("最高不超过2分"), prompt);
        assertTrue(prompt.contains("不得给到4-5分"), prompt); // 答案与引用完全无关 → 不上4-5
        assertTrue(prompt.contains("特殊规则"), prompt);       // 无引用片段 → 忠实度不扣分
        assertTrue(prompt.contains("【架构】分四层"), prompt);
    }

    @Test
    @DisplayName("grade: 无引用片段 → prompt 特殊规则明确（忠实度维度不扣分）")
    void grade_noSummariesPromptSpecialRule() {
        AtomicReference<AiEduChatRequest> captured = new AtomicReference<>();
        when(llmGateway.chat(any(AiEduChatRequest.class))).thenAnswer(inv -> {
            captured.set(inv.getArgument(0));
            return Mono.just(AiEduChatResponse.builder().response("{\"score\":3,\"reason\":\"ok\"}").build());
        });

        grader.grade("q", "a", List.of(), List.of()).block();

        String prompt = captured.get().getMessage();
        assertTrue(prompt.contains("（无引用片段）"), prompt);
        assertTrue(prompt.contains("忠实度维度不扣分"), prompt);
    }
}
