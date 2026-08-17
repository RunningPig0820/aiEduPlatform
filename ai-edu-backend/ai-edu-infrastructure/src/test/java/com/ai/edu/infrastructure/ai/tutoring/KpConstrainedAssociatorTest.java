package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.domain.llm.model.AiEduChatResponse;
import com.ai.edu.domain.llm.service.LlmGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 封闭域约束选择单测（test.md POOL-001~004）：LLM 只能从池里选，恒非空。
 */
class KpConstrainedAssociatorTest {

    private static final List<String> POOL = List.of("鸡兔同笼", "二元一次方程组", "相遇问题", "工程问题", "分式方程");

    private KpConstrainedAssociator associator;
    private LlmGateway llmGateway;

    @BeforeEach
    void setUp() {
        llmGateway = mock(LlmGateway.class);
        associator = new KpConstrainedAssociator();
        setField(associator, "llmGateway", llmGateway);
    }

    @Test
    @DisplayName("LLM 从池里选 top-N → 返回池内 label")
    void selectsFromPool() {
        when(llmGateway.chat(any())).thenReturn(Mono.just(AiEduChatResponse.builder()
                .response("鸡兔同笼\n二元一次方程组").build()));

        List<String> r = associator.associate("笼子里有鸡和兔共 35 个头，94 只脚", 4, POOL);

        assertEquals(List.of("鸡兔同笼", "二元一次方程组"), r);
        assertTrue(POOL.containsAll(r), "结果必须全在池内");
    }

    @Test
    @DisplayName("LLM 输出池外 → 过滤丢弃（跨学段错误被挡）")
    void filtersOutsidePool() {
        when(llmGateway.chat(any())).thenReturn(Mono.just(AiEduChatResponse.builder()
                .response("对数方程求解\n鸡兔同笼").build())); // 对数方程求解不在池内

        List<String> r = associator.associate("笼子里有鸡和兔共 35 个头，94 只脚", 4, POOL);

        assertEquals(List.of("鸡兔同笼"), r);
        assertTrue(POOL.containsAll(r));
    }

    @Test
    @DisplayName("池内排序确定性：LLM 顺序打乱 → 按池序稳定（top-1 不随 LLM 顺序变）")
    void sortsByPoolOrder() {
        when(llmGateway.chat(any())).thenReturn(Mono.just(AiEduChatResponse.builder()
                .response("二元一次方程组\n鸡兔同笼").build())); // LLM 顺序与池相反

        List<String> r = associator.associate("笼子里有鸡和兔共 35 个头，94 只脚", 4, POOL);

        assertEquals(List.of("鸡兔同笼", "二元一次方程组"), r); // 按池序（鸡兔同笼在池中更前）
    }

    @Test
    @DisplayName("LLM 失败/空 → 回退池前 N（恒非空）")
    void llmFailure_fallsBack() {
        when(llmGateway.chat(any())).thenThrow(new RuntimeException("llm down"));

        List<String> r = associator.associate("笼子里有鸡和兔共 35 个头，94 只脚", 4, POOL);

        assertFalse(r.isEmpty(), "恒非空");
        assertTrue(POOL.containsAll(r));
    }

    @Test
    @DisplayName("池空 → 返回空（调用方走 PENDING + keyword 兜底）")
    void emptyPool_returnsEmpty() {
        assertEquals(List.of(), associator.associate("笼子里有鸡和兔共 35 个头，94 只脚", 4, List.of()));
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
