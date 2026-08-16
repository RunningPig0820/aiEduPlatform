package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.domain.learning.model.valueobject.TopicKpHint;
import com.ai.edu.domain.llm.model.AiEduChatResponse;
import com.ai.edu.domain.llm.service.LlmGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 离线 LLM 自动关联（题型→知识点分布归纳）单测，覆盖 test.md AGG-004。
 */
class KpLlmTopicAggregatorTest {

    private KpLlmTopicAggregator aggregator;
    private LlmGateway llmGateway;

    @BeforeEach
    void setUp() {
        llmGateway = mock(LlmGateway.class);
        aggregator = new KpLlmTopicAggregator();
        setField(aggregator, "llmGateway", llmGateway);
    }

    @Test
    @DisplayName("单桶 → null（无需 LLM 归纳）")
    void singleHint_returnsNull() {
        assertNull(aggregator.refineDistribution("鸡兔同笼", List.of(hint("uri-1", 10))));
    }

    @Test
    @DisplayName("多桶 → LLM 归纳并归一化 ratio（和=1）")
    void multiHint_refinesAndNormalizes() {
        when(llmGateway.chat(any())).thenReturn(Mono.just(AiEduChatResponse.builder()
                .response("{\"distributions\":[{\"kp_uri\":\"uri-1\",\"ratio\":0.6},{\"kp_uri\":\"uri-2\",\"ratio\":0.3}]}")
                .build()));

        Map<String, Double> r = aggregator.refineDistribution("鸡兔同笼",
                List.of(hint("uri-1", 21), hint("uri-2", 38)));

        assertNotNull(r);
        assertEquals(2, r.size());
        assertEquals(0.6666, r.get("uri-1"), 0.001); // 0.6 / 0.9
        assertEquals(0.3333, r.get("uri-2"), 0.001); // 0.3 / 0.9
    }

    @Test
    @DisplayName("LLM 返回输入外 kp_uri → 剔除")
    void filtersUnknownUri() {
        when(llmGateway.chat(any())).thenReturn(Mono.just(AiEduChatResponse.builder()
                .response("{\"distributions\":[{\"kp_uri\":\"uri-1\",\"ratio\":0.8},{\"kp_uri\":\"hallucinated\",\"ratio\":0.2}]}")
                .build()));

        Map<String, Double> r = aggregator.refineDistribution("鸡兔同笼",
                List.of(hint("uri-1", 21), hint("uri-2", 38)));

        assertNotNull(r);
        assertEquals(1, r.size());
        assertEquals(1.0, r.get("uri-1"), 0.001); // 仅 uri-1，归一化后=1
    }

    @Test
    @DisplayName("LLM 失败 → null（降级纯计数）")
    void llmFailure_returnsNull() {
        when(llmGateway.chat(any())).thenThrow(new RuntimeException("llm down"));
        assertNull(aggregator.refineDistribution("鸡兔同笼", List.of(hint("uri-1", 21), hint("uri-2", 38))));
    }

    private TopicKpHint hint(String uri, int hits) {
        return TopicKpHint.builder().kpUri(uri).kpLabel("kp-" + uri).hitCount(hits).gradeRange(null).build();
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
