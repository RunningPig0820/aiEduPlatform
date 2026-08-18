package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.QuestionTypePageItemDTO;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.model.entity.QuestionTypeAlias;
import com.ai.edu.domain.learning.model.entity.QuestionTypeKp;
import com.ai.edu.domain.learning.model.valueobject.QuestionTypeStatus;
import com.ai.edu.domain.learning.repository.QuestionTypeAliasRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeKpRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 题型↔知识点维护应用服务单测（域 B 独立化 2.0.5：ADMIN 独立逻辑，替代 obs 共现自动涌现）。
 */
class KpQuestionTypeMaintenanceAppServiceTest {

    private KpQuestionTypeMaintenanceAppService service;
    private QuestionTypeRepository questionTypeRepository;
    private QuestionTypeKpRepository questionTypeKpRepository;
    private QuestionTypeAliasRepository questionTypeAliasRepository;

    @BeforeEach
    void setUp() {
        questionTypeRepository = mock(QuestionTypeRepository.class);
        questionTypeKpRepository = mock(QuestionTypeKpRepository.class);
        questionTypeAliasRepository = mock(QuestionTypeAliasRepository.class);
        service = new KpQuestionTypeMaintenanceAppService();
        service.setQuestionTypeRepository(questionTypeRepository);
        service.setQuestionTypeKpRepository(questionTypeKpRepository);
        service.setQuestionTypeAliasRepository(questionTypeAliasRepository);
    }

    @Test
    @DisplayName("upsertType 新建题型 → CANDIDATE + upsert")
    void upsertType_createsCandidate() {
        when(questionTypeRepository.findByTopicLabel("鸡兔同笼")).thenReturn(Optional.empty());
        when(questionTypeRepository.upsert(any())).thenAnswer(inv -> inv.getArgument(0));

        QuestionType qt = service.upsertType("鸡兔同笼");

        assertEquals("鸡兔同笼", qt.getTopicLabel());
        assertEquals(QuestionTypeStatus.CANDIDATE, qt.getStatus());
        verify(questionTypeRepository).upsert(any());
    }

    @Test
    @DisplayName("upsertType 已存在 → 幂等返回，不重建")
    void upsertType_existingIsIdempotent() {
        QuestionType existing = QuestionType.create("鸡兔同笼", QuestionTypeStatus.STABLE, 1001L);
        when(questionTypeRepository.findByTopicLabel("鸡兔同笼")).thenReturn(Optional.of(existing));

        QuestionType qt = service.upsertType("鸡兔同笼");

        assertEquals(existing, qt);
        verify(questionTypeRepository, never()).upsert(any());
    }

    @Test
    @DisplayName("promote 升 STABLE（手动审核，替代聚合阈值）")
    void promote_toStable() {
        QuestionType qt = QuestionType.create("鸡兔同笼", QuestionTypeStatus.CANDIDATE, 1001L);
        when(questionTypeRepository.findById(5L)).thenReturn(Optional.of(qt));
        when(questionTypeRepository.upsert(any())).thenAnswer(inv -> inv.getArgument(0));

        service.promote(5L, "鸡兔同笼问题 二元一次方程组");

        assertEquals(QuestionTypeStatus.STABLE, qt.getStatus());
        assertEquals("鸡兔同笼问题 二元一次方程组", qt.getDefinition());
        verify(questionTypeRepository).upsert(qt);
    }

    @Test
    @DisplayName("promote 题型不存在 → ENTITY_NOT_FOUND")
    void promote_notFound() {
        when(questionTypeRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.promote(99L, null));

        assertEquals(ErrorCode.ENTITY_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("bindKp 绑知识点分布桶（ratio/gradeRange 透传）")
    void bindKp_bindsDistribution() {
        when(questionTypeRepository.findById(5L)).thenReturn(Optional.of(QuestionType.create("鸡兔同笼", QuestionTypeStatus.STABLE, 1001L)));
        when(questionTypeKpRepository.upsert(any())).thenAnswer(inv -> inv.getArgument(0));

        QuestionTypeKp kp = service.bindKp(5L, "uri-A", 0.6, "4-6");

        assertEquals(5L, kp.getQuestionTypeId());
        assertEquals("uri-A", kp.getKpUri());
        assertEquals(0.6, kp.getRatio());
        assertEquals("4-6", kp.getGradeRange());
        verify(questionTypeKpRepository).upsert(any());
    }

    @Test
    @DisplayName("bindKp ratio 越界（>1）→ 参数错误")
    void bindKp_invalidRatio() {
        when(questionTypeRepository.findById(5L)).thenReturn(Optional.of(QuestionType.create("鸡兔同笼", QuestionTypeStatus.STABLE, 1001L)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.bindKp(5L, "uri-A", 1.5, null));

        assertEquals(ErrorCode.INVALID_PARAMS, ex.getCode());
    }

    @Test
    @DisplayName("addAlias 加变体别名 → canonical")
    void addAlias_mapsVariant() {
        when(questionTypeRepository.findById(5L)).thenReturn(Optional.of(QuestionType.create("鸡兔同笼", QuestionTypeStatus.STABLE, 1001L)));
        when(questionTypeAliasRepository.upsert(any())).thenAnswer(inv -> inv.getArgument(0));

        QuestionTypeAlias alias = service.addAlias(5L, "鸡兔同笼问题");

        assertEquals("鸡兔同笼问题", alias.getAliasLabel());
        assertEquals(5L, alias.getQuestionTypeId());
        verify(questionTypeAliasRepository).upsert(any());
    }
}
