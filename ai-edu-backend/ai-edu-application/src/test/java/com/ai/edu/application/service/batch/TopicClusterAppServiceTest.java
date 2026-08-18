package com.ai.edu.application.service.batch;

import com.ai.edu.application.dto.learning.TopicClusterResult;
import com.ai.edu.application.service.learning.TopicLabelAggregationService;
import com.ai.edu.domain.learning.model.entity.StudentQuestionRecord;
import com.ai.edu.domain.learning.model.entity.StudentTopicMastery;
import com.ai.edu.domain.learning.repository.StudentQuestionRecordRepository;
import com.ai.edu.domain.learning.repository.StudentTopicMasteryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 批量聚集 {@link TopicClusterAppService} 测试（tasks 2.6，手动触发非定时）。
 *
 * <p>覆盖：扫描未归并题型名 → 逐个聚集（归并/建锚）→ 更新题目表 canonical（幂等）→
 * 从题目表重算掌握表（按 student+canonical 累计平均）。
 */
class TopicClusterAppServiceTest {

    private StudentQuestionRecordRepository questionRepo;
    private StudentTopicMasteryRepository masteryRepo;
    private TopicLabelAggregationService aggregationService;
    private TopicClusterAppService service;

    @BeforeEach
    void setUp() {
        questionRepo = mock(StudentQuestionRecordRepository.class);
        masteryRepo = mock(StudentTopicMasteryRepository.class);
        aggregationService = mock(TopicLabelAggregationService.class);
        service = new TopicClusterAppService();
        inject(service, "questionRecordRepository", questionRepo);
        inject(service, "studentTopicMasteryRepository", masteryRepo);
        inject(service, "topicLabelAggregationService", aggregationService);
    }

    private StudentQuestionRecord record(Long studentId, String topic, String canonical, BigDecimal score) {
        return StudentQuestionRecord.create("ai", studentId, "题目" + topic, topic, canonical, score,
                0, 0, 1L, LocalDateTime.now());
    }

    // ---------- 批量聚集主流程 ----------

    @Test
    @DisplayName("批量聚集: 未归并题型名逐个聚集 → 更新题目表 canonical，归并计数正确")
    void cluster_mergesPendingTopics() {
        when(questionRepo.findPendingTopicLabels()).thenReturn(List.of("鸡兔同笼问题", "一元二次方成"));
        when(aggregationService.aggregate("鸡兔同笼问题", null)).thenReturn("鸡兔同笼");
        when(aggregationService.aggregate("一元二次方成", null)).thenReturn("一元二次方程");
        when(questionRepo.findAll()).thenReturn(List.of());

        TopicClusterResult result = service.cluster();

        assertEquals(2, result.getPendingTopics());
        assertEquals(2, result.getMergedTopics());
        verify(questionRepo).updateCanonicalByTopic("鸡兔同笼问题", "鸡兔同笼");
        verify(questionRepo).updateCanonicalByTopic("一元二次方成", "一元二次方程");
    }

    @Test
    @DisplayName("批量聚集: 建锚（canonical 同名）→ 填 canonical 但不计入归并数")
    void cluster_anchorFillsCanonicalNotCountedAsMerge() {
        when(questionRepo.findPendingTopicLabels()).thenReturn(List.of("鸡兔同笼"));
        when(aggregationService.aggregate("鸡兔同笼", null)).thenReturn("鸡兔同笼");
        when(questionRepo.findAll()).thenReturn(List.of());

        TopicClusterResult result = service.cluster();

        assertEquals(1, result.getPendingTopics());
        assertEquals(0, result.getMergedTopics());
        verify(questionRepo).updateCanonicalByTopic("鸡兔同笼", "鸡兔同笼");
    }

    @Test
    @DisplayName("幂等: 无未归并题型名 → 无更新/无重算")
    void cluster_idempotent_whenNoPending() {
        when(questionRepo.findPendingTopicLabels()).thenReturn(List.of());
        when(questionRepo.findAll()).thenReturn(List.of());

        TopicClusterResult result = service.cluster();

        assertEquals(0, result.getPendingTopics());
        verify(questionRepo, never()).updateCanonicalByTopic(anyString(), anyString());
        verify(masteryRepo, never()).upsert(any());
    }

    // ---------- 重算掌握表（2.6.3） ----------

    @Test
    @DisplayName("重算掌握表: 题目表按 student+canonical 累计平均，归并后的散题聚到 canonical")
    void recomputeMastery_cumulativeFromRecords() {
        when(questionRepo.findPendingTopicLabels()).thenReturn(List.of());
        when(questionRepo.findAll()).thenReturn(List.of(
                record(1001L, "鸡兔同笼", "鸡兔同笼", new BigDecimal("0.7")),
                record(1001L, "鸡兔同笼问题", "鸡兔同笼", new BigDecimal("1.0")),  // 归并后同一 canonical
                record(1002L, "一元二次方程", "一元二次方程", new BigDecimal("0.5"))));

        service.cluster();

        verify(masteryRepo).upsert(org.mockito.ArgumentMatchers.argThat(
                m -> m instanceof StudentTopicMastery mastery
                        && mastery.getStudentId() == 1001L
                        && "鸡兔同笼".equals(mastery.getTopicLabel())
                        && mastery.getTrainCount() == 2
                        && mastery.getMasteryLevel().getValue() == 85));  // (0.7+1.0) 累计平均 → 85%
        verify(masteryRepo).upsert(org.mockito.ArgumentMatchers.argThat(
                m -> m instanceof StudentTopicMastery mastery
                        && mastery.getStudentId() == 1002L
                        && "一元二次方程".equals(mastery.getTopicLabel())
                        && mastery.getTrainCount() == 1
                        && mastery.getMasteryLevel().getValue() == 50));
    }

    @Test
    @DisplayName("重算掌握表: PENDING（canonical 空）题目不参与聚合")
    void recomputeMastery_skipsPending() {
        when(questionRepo.findPendingTopicLabels()).thenReturn(List.of());
        when(questionRepo.findAll()).thenReturn(List.of(
                record(1001L, "未识别题", null, new BigDecimal("1.0"))));

        service.cluster();

        verify(masteryRepo, never()).upsert(any());
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
