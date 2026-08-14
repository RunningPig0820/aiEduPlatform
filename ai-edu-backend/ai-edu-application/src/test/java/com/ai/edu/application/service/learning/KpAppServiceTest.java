package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.ConfirmKpAliasDTO;
import com.ai.edu.application.dto.learning.KpResolveDTO;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.learning.model.valueobject.KpResolution;
import com.ai.edu.domain.learning.repository.DerivedKpObsRepository;
import com.ai.edu.domain.learning.service.TutoringKpResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 知识点解析与挂起审核应用服务单测（mock 端口/仓储，验证 DTO 转换 + 确认校验）。
 */
class KpAppServiceTest {

    private static final String KP_URI = "http://edukg.org/knowledge/3.1/kp/math#jsfa";

    private KpAppService service;
    private TutoringKpResolver kpResolver;
    private DerivedKpObsRepository obsRepository;
    private KgKnowledgePointRepository kgRepository;

    @BeforeEach
    void setUp() {
        kpResolver = mock(TutoringKpResolver.class);
        obsRepository = mock(DerivedKpObsRepository.class);
        kgRepository = mock(KgKnowledgePointRepository.class);
        service = new KpAppService();
        setField(service, "kpResolver", kpResolver);
        setField(service, "derivedKpObsRepository", obsRepository);
        setField(service, "kgKnowledgePointRepository", kgRepository);
    }

    @Test
    @DisplayName("resolve：KpResolution → DTO（含候选）")
    void resolveMapsToDto() {
        when(kpResolver.resolve("鸡兔同笼", 101L))
                .thenReturn(KpResolution.resolved("鸡兔同笼", KP_URI, "假设法", 88));

        KpResolveDTO dto = service.resolve("鸡兔同笼", 101L);

        assertEquals(KP_URI, dto.getUri());
        assertEquals("假设法", dto.getKpLabel());
        assertEquals(88, dto.getConfidence());
        assertEquals(KpResolution.STATUS_RESOLVED, dto.getStatus());
    }

    @Test
    @DisplayName("confirm：kp_uri 不在镜像 → INVALID_PARAMS")
    void confirmInvalidUri() {
        when(kgRepository.findByUri("invalid")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.confirm(1L, "invalid"));
        assertEquals(ErrorCode.INVALID_PARAMS, ex.getCode());
    }

    @Test
    @DisplayName("confirm：观测不存在 → 50007")
    void confirmObsNotFound() {
        when(kgRepository.findByUri(KP_URI)).thenReturn(Optional.of(KgKnowledgePoint.create(KP_URI, "假设法")));
        when(obsRepository.confirm(1L, KP_URI)).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.confirm(1L, KP_URI));
        assertEquals(ErrorCode.KP_OBS_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("confirm：成功 → updated=true")
    void confirmSuccess() {
        when(kgRepository.findByUri(KP_URI)).thenReturn(Optional.of(KgKnowledgePoint.create(KP_URI, "假设法")));
        when(obsRepository.confirm(1L, KP_URI)).thenReturn(1);

        ConfirmKpAliasDTO result = service.confirm(1L, KP_URI);

        assertTrue(result.isUpdated());
        assertEquals("RESOLVED", result.getStatus());
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
