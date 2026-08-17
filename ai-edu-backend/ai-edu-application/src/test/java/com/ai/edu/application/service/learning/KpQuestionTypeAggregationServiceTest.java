package com.ai.edu.application.service.learning;

import com.ai.edu.application.service.batch.KpQuestionTypeAggregationService;
import com.ai.edu.domain.learning.model.entity.DerivedKpObs;
import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.model.entity.QuestionTypeAlias;
import com.ai.edu.domain.learning.model.entity.QuestionTypeKp;
import com.ai.edu.domain.learning.model.valueobject.DerivedKpSource;
import com.ai.edu.domain.learning.model.valueobject.DerivedKpStatus;
import com.ai.edu.domain.learning.model.valueobject.QuestionTypeStatus;
import com.ai.edu.domain.learning.repository.DerivedKpObsRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeAliasRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeKpRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 题型库聚合服务单测（mock 仓储，验证聚合阈值 + 升 STABLE 门槛 + 变体别名合并）。
 */
class KpQuestionTypeAggregationServiceTest {

    private static final String KP = "http://edukg.org/knowledge/3.1/kp/math#jsfa";

    private KpQuestionTypeAggregationService service;
    private DerivedKpObsRepository obsRepository;
    private QuestionTypeRepository questionTypeRepository;
    private QuestionTypeKpRepository questionTypeKpRepository;
    private QuestionTypeAliasRepository questionTypeAliasRepository;

    @BeforeEach
    void setUp() {
        obsRepository = mock(DerivedKpObsRepository.class);
        questionTypeRepository = mock(QuestionTypeRepository.class);
        questionTypeKpRepository = mock(QuestionTypeKpRepository.class);
        questionTypeAliasRepository = mock(QuestionTypeAliasRepository.class);
        service = new KpQuestionTypeAggregationService();
        setField(service, "derivedKpObsRepository", obsRepository);
        setField(service, "questionTypeRepository", questionTypeRepository);
        setField(service, "questionTypeKpRepository", questionTypeKpRepository);
        setField(service, "questionTypeAliasRepository", questionTypeAliasRepository);
        setField(service, "candidateStudents", 3);
        setField(service, "candidateHits", 5);
        setField(service, "stableStudents", 10);
        setField(service, "aliasOverlap", 0.7);
    }

    @Test
    @DisplayName("达阈值（≥3 学生且 ≥5 命中）→ 建 CANDIDATE + 分布桶")
    void aggregateReachesThreshold() {
        List<DerivedKpObs> all = List.of(obs(1L, 4), obs(2L, 4), obs(3L, 5), obs(4L, 5), obs(5L, 6));
        when(obsRepository.findResolved()).thenReturn(all);
        when(obsRepository.findResolvedByTopicLabels(any())).thenReturn(all);
        when(questionTypeRepository.findAll()).thenReturn(List.of());
        when(questionTypeKpRepository.findAll()).thenReturn(List.of());
        when(questionTypeRepository.findByTopicLabelOrAlias("鸡兔同笼")).thenReturn(Optional.empty());
        when(questionTypeAliasRepository.findByQuestionTypeId(any())).thenReturn(List.of());
        QuestionType qt = QuestionType.create("鸡兔同笼", QuestionTypeStatus.CANDIDATE, 1L);
        qt.setId(100L);
        when(questionTypeRepository.upsert(any())).thenReturn(qt);

        service.aggregate();

        // 新建 canonical：create + rebuild 各 upsert 一次
        verify(questionTypeRepository, atLeastOnce()).upsert(any(QuestionType.class));
        verify(questionTypeKpRepository, atLeastOnce()).upsert(any());
    }

    @Test
    @DisplayName("未达阈值（仅 1 学生）→ 不聚合")
    void aggregateBelowThreshold() {
        when(obsRepository.findResolved()).thenReturn(List.of(obs(1L, 4)));
        when(questionTypeRepository.findAll()).thenReturn(List.of());
        when(questionTypeKpRepository.findAll()).thenReturn(List.of());

        service.aggregate();

        verify(questionTypeRepository, never()).upsert(any());
        verify(questionTypeKpRepository, never()).upsert(any());
        verify(questionTypeAliasRepository, never()).upsert(any());
    }

    @Test
    @DisplayName("变体 kp 重叠 ≥70% → 合并进 canonical + 别名 + union 统计合并")
    void variantMergesIntoCanonical() {
        QuestionType canonical = QuestionType.create("鸡兔同笼问题", QuestionTypeStatus.CANDIDATE, 1L);
        canonical.setId(5L);
        List<DerivedKpObs> canonicalObs = List.of(
                obs(1L, "鸡兔同笼问题", KP), obs(2L, "鸡兔同笼问题", KP), obs(3L, "鸡兔同笼问题", KP),
                obs(4L, "鸡兔同笼问题", KP), obs(5L, "鸡兔同笼问题", KP));
        List<DerivedKpObs> variantObs = List.of(
                obs(6L, "鸡兔同笼", KP), obs(7L, "鸡兔同笼", KP), obs(8L, "鸡兔同笼", KP),
                obs(9L, "鸡兔同笼", KP), obs(10L, "鸡兔同笼", KP));
        List<DerivedKpObs> union = new java.util.ArrayList<>(canonicalObs);
        union.addAll(variantObs);

        when(obsRepository.findResolved()).thenReturn(union);
        when(questionTypeRepository.findAll()).thenReturn(List.of(canonical));
        when(questionTypeKpRepository.findAll()).thenReturn(List.of(bucket(5L, KP)));
        when(questionTypeRepository.findByTopicLabelOrAlias("鸡兔同笼问题")).thenReturn(Optional.of(canonical));
        when(questionTypeRepository.findByTopicLabelOrAlias("鸡兔同笼")).thenReturn(Optional.empty());
        when(questionTypeAliasRepository.findByQuestionTypeId(5L)).thenReturn(List.of());
        when(obsRepository.findResolvedByTopicLabels(any())).thenReturn(union);
        when(questionTypeRepository.upsert(any())).thenReturn(canonical);

        service.aggregate();

        // 变体插入别名 → canonical
        verify(questionTypeAliasRepository).upsert(argThat(a ->
                "鸡兔同笼".equals(a.getAliasLabel()) && 5L == a.getQuestionTypeId()));
        // canonical 统计按 union 合并（10 学生），且不新建第二条目
        verify(questionTypeRepository).upsert(argThat(t -> t.getId() == 5L && t.getHitStudents() == 10));
        verify(questionTypeRepository, atMost(1)).upsert(any());
    }

    @Test
    @DisplayName("无 kp 重叠相似 → 新建独立 CANDIDATE")
    void noSimilar_createsNew() {
        QuestionType other = QuestionType.create("相遇问题", QuestionTypeStatus.CANDIDATE, 1L);
        other.setId(9L);
        List<DerivedKpObs> newObs = List.of(
                obs(1L, "鸡兔同笼", KP), obs(2L, "鸡兔同笼", KP), obs(3L, "鸡兔同笼", KP),
                obs(4L, "鸡兔同笼", KP), obs(5L, "鸡兔同笼", KP));
        when(obsRepository.findResolved()).thenReturn(newObs);
        when(questionTypeRepository.findAll()).thenReturn(List.of(other));
        // other 的 kp 集合与"鸡兔同笼"完全不相交 → 重叠 0 < 0.7
        when(questionTypeKpRepository.findAll()).thenReturn(List.of(bucket(9L, "uri-different")));
        when(questionTypeRepository.findByTopicLabelOrAlias("鸡兔同笼")).thenReturn(Optional.empty());
        when(questionTypeAliasRepository.findByQuestionTypeId(any())).thenReturn(List.of());
        when(obsRepository.findResolvedByTopicLabels(any())).thenReturn(newObs);
        QuestionType created = QuestionType.create("鸡兔同笼", QuestionTypeStatus.CANDIDATE, 1L);
        created.setId(100L);
        when(questionTypeRepository.upsert(any())).thenReturn(created);

        service.aggregate();

        // 新建 canonical（鸡兔同笼，rebuild 时 id 已回填），且不落别名
        verify(questionTypeRepository).upsert(argThat(t ->
                t != null && t.getId() != null && "鸡兔同笼".equals(t.getTopicLabel())));
        verify(questionTypeAliasRepository, never()).upsert(any());
    }

    @Test
    @DisplayName("变体不足阈值 → 不落别名（不把噪声折进 canonical）")
    void underThresholdVariant_noAlias() {
        QuestionType canonical = QuestionType.create("鸡兔同笼问题", QuestionTypeStatus.CANDIDATE, 1L);
        canonical.setId(5L);
        List<DerivedKpObs> canonicalObs = List.of(
                obs(1L, "鸡兔同笼问题", KP), obs(2L, "鸡兔同笼问题", KP), obs(3L, "鸡兔同笼问题", KP),
                obs(4L, "鸡兔同笼问题", KP), obs(5L, "鸡兔同笼问题", KP));
        List<DerivedKpObs> variantObs = List.of(obs(6L, "鸡兔同笼", KP), obs(7L, "鸡兔同笼", KP)); // 2 学生 < 3
        List<DerivedKpObs> union = new java.util.ArrayList<>(canonicalObs);
        union.addAll(variantObs);

        when(obsRepository.findResolved()).thenReturn(union);
        when(questionTypeRepository.findAll()).thenReturn(List.of(canonical));
        when(questionTypeKpRepository.findAll()).thenReturn(List.of(bucket(5L, KP)));
        when(questionTypeRepository.findByTopicLabelOrAlias("鸡兔同笼问题")).thenReturn(Optional.of(canonical));
        when(questionTypeRepository.findByTopicLabelOrAlias("鸡兔同笼")).thenReturn(Optional.empty());
        when(questionTypeAliasRepository.findByQuestionTypeId(5L)).thenReturn(List.of());
        when(obsRepository.findResolvedByTopicLabels(any())).thenReturn(canonicalObs);
        when(questionTypeRepository.upsert(any())).thenReturn(canonical);

        service.aggregate();

        verify(questionTypeAliasRepository, never()).upsert(any());
        verify(questionTypeRepository).upsert(argThat(t -> t.getId() == 5L && t.getHitStudents() == 5));
    }

    @Test
    @DisplayName("升 STABLE：<10 学生 → 拒绝")
    void promoteToStableRejected() {
        QuestionType qt = QuestionType.create("鸡兔同笼", QuestionTypeStatus.CANDIDATE, 1L);
        qt.updateStats(5, 10);
        when(questionTypeRepository.findByTopicLabel("鸡兔同笼")).thenReturn(Optional.of(qt));

        assertThrows(IllegalStateException.class, () -> service.promoteToStable("鸡兔同笼", null));
        verify(questionTypeRepository, never()).upsert(any());
    }

    @Test
    @DisplayName("升 STABLE：≥10 学生 → 转 STABLE")
    void promoteToStableSucceeds() {
        QuestionType qt = QuestionType.create("鸡兔同笼", QuestionTypeStatus.CANDIDATE, 1L);
        qt.updateStats(12, 30);
        when(questionTypeRepository.findByTopicLabel("鸡兔同笼")).thenReturn(Optional.of(qt));

        service.promoteToStable("鸡兔同笼", "已知鸡兔总数与脚数求各多少");

        verify(questionTypeRepository).upsert(argThat(t -> t.getStatus() == QuestionTypeStatus.STABLE));
    }

    private DerivedKpObs obs(Long studentId, int grade) {
        return obs(studentId, "鸡兔同笼", KP, grade);
    }

    private DerivedKpObs obs(Long studentId, String topic, String kpUri) {
        return obs(studentId, topic, kpUri, null);
    }

    private DerivedKpObs obs(Long studentId, String topic, String kpUri, Integer grade) {
        DerivedKpObs o = DerivedKpObs.create(studentId, topic, kpUri, grade, 80,
                DerivedKpSource.LLM, DerivedKpStatus.RESOLVED);
        o.setId(studentId);
        return o;
    }

    private QuestionTypeKp bucket(Long questionTypeId, String kpUri) {
        return QuestionTypeKp.create(questionTypeId, kpUri, null);
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
