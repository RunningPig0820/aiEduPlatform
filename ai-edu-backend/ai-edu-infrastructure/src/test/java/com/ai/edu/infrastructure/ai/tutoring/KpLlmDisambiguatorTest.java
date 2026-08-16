package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.learning.model.valueobject.KpResolution;
import com.ai.edu.domain.llm.model.AiEduChatResponse;
import com.ai.edu.domain.llm.service.LlmGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 冷启动 LLM 消歧（两段式：生成候选名 + 镜像校验）单测，覆盖 test.md RES-013/014。
 */
class KpLlmDisambiguatorTest {

    private KpLlmDisambiguator disambiguator;
    private LlmGateway llmGateway;
    private KgKnowledgePointRepository kgRepository;

    @BeforeEach
    void setUp() {
        llmGateway = mock(LlmGateway.class);
        kgRepository = mock(KgKnowledgePointRepository.class);
        disambiguator = new KpLlmDisambiguator();
        setField(disambiguator, "llmGateway", llmGateway);
        setField(disambiguator, "kgKnowledgePointRepository", kgRepository);
    }

    @Test
    @DisplayName("单候选名命中镜像 → RESOLVED")
    void singleCandidateResolved() {
        when(llmGateway.chat(any())).thenReturn(Mono.just(AiEduChatResponse.builder().response("二元一次方程组").build()));
        when(kgRepository.findByLabel("二元一次方程组")).thenReturn(Optional.of(kp("uri-1", "二元一次方程组")));

        KpResolution r = disambiguator.disambiguate("鸡兔同笼", null);

        assertEquals(KpResolution.STATUS_RESOLVED, r.getStatus());
        assertEquals("uri-1", r.getUri());
    }

    @Test
    @DisplayName("多候选名命中镜像 → PENDING 携带候选（弹澄清卡）")
    void multipleCandidatesPending() {
        when(llmGateway.chat(any()))
                .thenReturn(Mono.just(AiEduChatResponse.builder().response("二元一次方程组\n假设法").build()));
        when(kgRepository.findByLabel("二元一次方程组")).thenReturn(Optional.of(kp("uri-1", "二元一次方程组")));
        when(kgRepository.findByLabel("假设法")).thenReturn(Optional.of(kp("uri-2", "假设法")));

        KpResolution r = disambiguator.disambiguate("鸡兔同笼", null);

        assertEquals(KpResolution.STATUS_PENDING, r.getStatus());
        assertTrue(r.getCandidates().contains("二元一次方程组"));
        assertTrue(r.getCandidates().contains("假设法"));
    }

    @Test
    @DisplayName("候选名全未命中镜像 → null（挂起无候选）")
    void allUnverified() {
        when(llmGateway.chat(any()))
                .thenReturn(Mono.just(AiEduChatResponse.builder().response("鸡兔同笼定理").build()));
        when(kgRepository.findByLabel("鸡兔同笼定理")).thenReturn(Optional.empty());
        when(kgRepository.findByLabelLike("鸡兔同笼定理")).thenReturn(Optional.empty());

        assertNull(disambiguator.disambiguate("鸡兔同笼", null));
    }

    @Test
    @DisplayName("候选名精确未命中但 LIKE 命中 → RESOLVED")
    void likeFallback() {
        when(llmGateway.chat(any()))
                .thenReturn(Mono.just(AiEduChatResponse.builder().response("二元一次方程组").build()));
        when(kgRepository.findByLabel("二元一次方程组")).thenReturn(Optional.empty());
        when(kgRepository.findByLabelLike("二元一次方程组"))
                .thenReturn(Optional.of(kp("uri-1", "章前引言和二元一次方程组")));

        KpResolution r = disambiguator.disambiguate("鸡兔同笼", null);

        assertEquals(KpResolution.STATUS_RESOLVED, r.getStatus());
        assertEquals("uri-1", r.getUri());
    }

    @Test
    @DisplayName("LLM 失败 → null（降级挂起）")
    void llmFailure() {
        when(llmGateway.chat(any())).thenThrow(new RuntimeException("llm down"));
        assertNull(disambiguator.disambiguate("鸡兔同笼", null));
    }

    private KgKnowledgePoint kp(String uri, String label) {
        return KgKnowledgePoint.create(uri, label);
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
