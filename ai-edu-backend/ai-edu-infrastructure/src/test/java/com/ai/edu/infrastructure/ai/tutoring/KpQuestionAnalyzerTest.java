package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import com.ai.edu.domain.llm.model.AiEduChatRequest;
import com.ai.edu.domain.llm.model.AiEduChatResponse;
import com.ai.edu.domain.llm.service.LlmGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 题目理解（题目文本 → 候选题型名）单测，覆盖 test.md UND-001/002/003。
 */
class KpQuestionAnalyzerTest {

    private KpQuestionAnalyzer analyzer;
    private LlmGateway llmGateway;
    private QuestionTypeRepository questionTypeRepository;

    @BeforeEach
    void setUp() {
        llmGateway = mock(LlmGateway.class);
        questionTypeRepository = mock(QuestionTypeRepository.class);
        analyzer = new KpQuestionAnalyzer();
        setField(analyzer, "llmGateway", llmGateway);
        setField(analyzer, "questionTypeRepository", questionTypeRepository);
    }

    @Test
    @DisplayName("注入题型库参考词表并返回候选题型名")
    void injectsKnownVocabularyAndParses() {
        when(questionTypeRepository.findTopTopicLabels(20)).thenReturn(List.of("鸡兔同笼", "相遇问题"));
        when(llmGateway.chat(any())).thenReturn(Mono.just(AiEduChatResponse.builder().response("鸡兔同笼").build()));

        List<String> r = analyzer.understand("笼子里有鸡和兔共 35 个头，94 只脚", null);

        ArgumentCaptor<AiEduChatRequest> captor = ArgumentCaptor.forClass(AiEduChatRequest.class);
        verify(llmGateway).chat(captor.capture());
        assertTrue(captor.getValue().getMessage().contains("鸡兔同笼"), "prompt 应注入题型库参考词表");
        assertEquals(List.of("鸡兔同笼"), r);
    }

    @Test
    @DisplayName("多候选解析去编号/bullet")
    void multiCandidateStripsNumbering() {
        when(questionTypeRepository.findTopTopicLabels(20)).thenReturn(List.of());
        when(llmGateway.chat(any())).thenReturn(Mono.just(AiEduChatResponse.builder()
                .response("1. 鸡兔同笼\n2. 假设法\n- 方程法").build()));

        List<String> r = analyzer.understand("笼子里有鸡和兔共 35 个头，94 只脚", null);

        assertEquals(List.of("鸡兔同笼", "假设法", "方程法"), r);
    }

    @Test
    @DisplayName("LLM 失败返回空列表（调用方降级 PENDING）")
    void llmFailure_returnsEmpty() {
        when(questionTypeRepository.findTopTopicLabels(20)).thenReturn(List.of());
        when(llmGateway.chat(any())).thenThrow(new RuntimeException("llm down"));

        assertEquals(List.of(), analyzer.understand("笼子里有鸡和兔共 35 个头，94 只脚", null));
    }

    @Test
    @DisplayName("LLM 空响应返回空列表")
    void emptyResponse_returnsEmpty() {
        when(questionTypeRepository.findTopTopicLabels(20)).thenReturn(List.of());
        when(llmGateway.chat(any())).thenReturn(Mono.just(AiEduChatResponse.builder().response("  ").build()));

        assertEquals(List.of(), analyzer.understand("笼子里有鸡和兔共 35 个头，94 只脚", null));
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
