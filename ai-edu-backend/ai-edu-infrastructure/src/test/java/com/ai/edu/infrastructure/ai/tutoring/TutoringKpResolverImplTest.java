package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.infrastructure.persistence.edukg.mapper.KgKnowledgePointMapper;
import com.ai.edu.infrastructure.persistence.edukg.po.KgKnowledgePointPo;
import com.ai.edu.infrastructure.persistence.edukg.util.EntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 知识点 label → TextbookKP URI 解析实现单测（mock kg 镜像 Mapper）。
 */
class TutoringKpResolverImplTest {

    private static final String KP_URI = "http://edukg.org/knowledge/3.1/textbook/kp1";

    private TutoringKpResolverImpl resolver;
    private KgKnowledgePointMapper kgKnowledgePointMapper;

    @BeforeEach
    void setUp() {
        kgKnowledgePointMapper = mock(KgKnowledgePointMapper.class);
        resolver = new TutoringKpResolverImpl();
        setField(resolver, "kgKnowledgePointMapper", kgKnowledgePointMapper);
    }

    @Test
    @DisplayName("精确匹配命中 → 返回 URI")
    void exactMatch() {
        when(kgKnowledgePointMapper.selectByLabel("二元一次方程组")).thenReturn(kp(KP_URI));

        String uri = resolver.resolveLabelToUri("二元一次方程组");

        assertEquals(KP_URI, uri);
        verify(kgKnowledgePointMapper, never()).selectByLabelLike(anyString());
    }

    @Test
    @DisplayName("精确未命中 → LIKE 模糊命中 → 返回 URI")
    void likeMatchFallback() {
        when(kgKnowledgePointMapper.selectByLabel("二元一次方程组")).thenReturn(null);
        when(kgKnowledgePointMapper.selectByLabelLike("二元一次方程组")).thenReturn(kp(KP_URI));

        String uri = resolver.resolveLabelToUri("二元一次方程组");

        assertEquals(KP_URI, uri);
    }

    @Test
    @DisplayName("精确/LIKE 均未命中 → null（待收录，不点亮）")
    void noMatch() {
        when(kgKnowledgePointMapper.selectByLabel(anyString())).thenReturn(null);
        when(kgKnowledgePointMapper.selectByLabelLike(anyString())).thenReturn(null);

        assertNull(resolver.resolveLabelToUri("不存在的知识点"));
    }

    @Test
    @DisplayName("空 label → 直接 null，不查库")
    void blankLabel() {
        assertNull(resolver.resolveLabelToUri(null));
        assertNull(resolver.resolveLabelToUri("  "));
        verify(kgKnowledgePointMapper, never()).selectByLabel(anyString());
    }

    private KgKnowledgePointPo kp(String uri) {
        KgKnowledgePointPo po = EntityFactory.create(KgKnowledgePointPo.class);
        setField(po, "uri", uri);
        return po;
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
