package com.ai.edu.application.service.learning;

import com.ai.edu.common.exception.TutoringAgentException;
import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.model.entity.QuestionTypeAlias;
import com.ai.edu.domain.learning.model.valueobject.QuestionTypeStatus;
import com.ai.edu.domain.learning.model.contract.TopicVectorMetadata;
import com.ai.edu.domain.learning.model.contract.TopicVectorNeighbor;
import com.ai.edu.domain.learning.model.contract.TopicVectorPutRequest;
import com.ai.edu.domain.learning.repository.QuestionTypeAliasRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import com.ai.edu.domain.learning.service.TopicVectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 题型聚集编排 {@link TopicLabelAggregationService} 测试（tasks 2.5，test.md NOR-002~007）。
 *
 * <p>覆盖判定链路：字符规则池归并（免向量）→ 向量最近邻归并（写别名）→ 阈值边界
 * （中阈值保守建新不误并 / 异型建新）→ 首题建锚（零锚点）→ 向量不可用兜底回退字符规则。
 */
class TopicLabelAggregationServiceTest {

    private static final Long STUDENT_ID = 1001L;

    private TopicVectorStore vectorStore;
    private QuestionTypeRepository qtRepo;
    private QuestionTypeAliasRepository aliasRepo;
    private TopicLabelAggregationService service;

    @BeforeEach
    void setUp() {
        vectorStore = mock(TopicVectorStore.class);
        qtRepo = mock(QuestionTypeRepository.class);
        aliasRepo = mock(QuestionTypeAliasRepository.class);
        service = new TopicLabelAggregationService();
        inject(service, "topicVectorStore", vectorStore);
        inject(service, "questionTypeRepository", qtRepo);
        inject(service, "questionTypeAliasRepository", aliasRepo);

        when(qtRepo.upsert(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aliasRepo.upsert(any())).thenAnswer(inv -> inv.getArgument(0));
        when(qtRepo.findByTopicLabel(anyString())).thenReturn(Optional.empty());
    }

    private QuestionType type(long id, String label) {
        return QuestionType.restore(id, label, QuestionTypeStatus.CANDIDATE, null, 1, 1, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private TopicVectorNeighbor neighbor(double distance, String canonicalLabel) {
        return TopicVectorNeighbor.builder()
                .key("topic_" + canonicalLabel)
                .metadata(TopicVectorMetadata.builder().canonicalLabel(canonicalLabel).build())
                .distance(distance)
                .build();
    }

    // ---------- 向量最近邻归并（NOR-002） ----------

    @Test
    @DisplayName("NOR-002: 向量命中归并（distance 0.077 ≤ 0.2）→ 返回 canonical + 写别名表")
    void vectorHit_mergesAndWritesAlias() {
        when(vectorStore.queryNearestTop1("鸡兔同笼问题"))
                .thenReturn(Optional.of(neighbor(0.077, "鸡兔同笼")));
        when(qtRepo.findByTopicLabel("鸡兔同笼")).thenReturn(Optional.of(type(1L, "鸡兔同笼")));

        String canonical = service.aggregate("鸡兔同笼问题", STUDENT_ID);

        assertEquals("鸡兔同笼", canonical);
        verify(aliasRepo).upsert(org.mockito.ArgumentMatchers.argThat(
                a -> a instanceof QuestionTypeAlias alias
                        && "鸡兔同笼问题".equals(alias.getAliasLabel())
                        && alias.getQuestionTypeId() == 1L));
        verify(vectorStore, never()).putVector(any());
    }

    // 字符规则池归并（编辑距离 ≤1）已移除（拍板）：近字/错别字归并全交给向量层，
    // 见 TopicLabelRuleNormalizer 类注释。NOR-007「不裂行」由「解/求前缀剥离」+ 向量归并保证。

    // ---------- 阈值边界（NOR-004 / 中阈值） ----------

    @Test
    @DisplayName("NOR-004: 异型 distance 0.5 ≥ 0.3 → 不归并，建新 canonical + 向量入库")
    void farDistance_createsNew() {
        when(vectorStore.queryNearestTop1("相遇问题")).thenReturn(Optional.of(neighbor(0.5, "鸡兔同笼")));

        String canonical = service.aggregate("相遇问题", STUDENT_ID);

        assertEquals("相遇问题", canonical);
        verify(qtRepo).upsert(org.mockito.ArgumentMatchers.argThat(
                q -> q instanceof QuestionType qt && "相遇问题".equals(qt.getTopicLabel())));
        verify(vectorStore).putVector(org.mockito.ArgumentMatchers.argThat(
                r -> r instanceof TopicVectorPutRequest req
                        && "topic_相遇问题".equals(req.getKey())
                        && "相遇问题".equals(req.getMetadata().getCanonicalLabel())));
        verify(aliasRepo, never()).upsert(any());
    }

    @Test
    @DisplayName("中阈值区间: distance 0.25（>0.2 <0.3）→ 保守建新不误并（候选仲裁待接，宁可拆）")
    void midThreshold_createsNewNoMerge() {
        when(vectorStore.queryNearestTop1("假设法")).thenReturn(Optional.of(neighbor(0.25, "鸡兔同笼")));

        String canonical = service.aggregate("假设法", STUDENT_ID);

        assertEquals("假设法", canonical);
        verify(aliasRepo, never()).upsert(any());
        verify(qtRepo).upsert(org.mockito.ArgumentMatchers.argThat(
                q -> q instanceof QuestionType qt && "假设法".equals(qt.getTopicLabel())));
    }

    // ---------- 首题建锚（NOR-005） ----------

    @Test
    @DisplayName("NOR-005: 无近邻（库空/put 未异步生效）→ 首题建锚 canonical + 题型名向量入库")
    void anchor_createdOnEmptyNeighbor() {
        when(vectorStore.queryNearestTop1("鸡兔同笼")).thenReturn(Optional.empty());

        String canonical = service.aggregate("鸡兔同笼", STUDENT_ID);

        assertEquals("鸡兔同笼", canonical);
        verify(qtRepo).upsert(org.mockito.ArgumentMatchers.argThat(
                q -> q instanceof QuestionType qt
                        && "鸡兔同笼".equals(qt.getTopicLabel())
                        && qt.getStatus() == QuestionTypeStatus.CANDIDATE));
        verify(vectorStore).putVector(org.mockito.ArgumentMatchers.argThat(
                r -> r instanceof TopicVectorPutRequest req
                        && "topic_鸡兔同笼".equals(req.getKey())
                        && String.valueOf(STUDENT_ID).equals(req.getMetadata().getStudentId())
                        && "鸡兔同笼".equals(req.getMetadata().getCanonicalLabel())));
    }

    // ---------- 失败兜底（NOR-006） ----------

    @Test
    @DisplayName("NOR-006: 向量库不可用 → 回退字符规则结果，不阻塞（原样落库由调用方）")
    void vectorDown_fallsBackToCharRule() {
        // 实现先 normalize 再查向量：内部调用 queryNearestTop1("一元二次方程")（非原始名）
        when(vectorStore.queryNearestTop1(anyString()))
                .thenThrow(new TutoringAgentException("题型名向量检索服务暂不可用"));

        String canonical = service.aggregate("解一元二次方程", STUDENT_ID);

        assertEquals("一元二次方程", canonical, "回退字符规则归一结果，不建锚不归并");
        verify(qtRepo, never()).upsert(any());
        verify(vectorStore, never()).putVector(any());
        verify(aliasRepo, never()).upsert(any());
    }

    // ---------- 空输入 ----------

    @Test
    @DisplayName("空输入 → 原样返回，不触发任何链路")
    void nullOrBlank_returnsAsIs() {
        assertNull(service.aggregate(null, STUDENT_ID));
        assertEquals("  ", service.aggregate("  ", STUDENT_ID));
        verify(vectorStore, never()).queryNearestTop1(anyString());
        verify(vectorStore, never()).putVector(any());
    }

    private void inject(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
