package com.ai.edu.application.service.batch;

import com.ai.edu.application.dto.learning.TopicClusterResult;
import com.ai.edu.application.service.learning.TopicLabelAggregationService;
import com.ai.edu.domain.learning.model.entity.StudentQuestionRecord;
import com.ai.edu.domain.learning.model.entity.StudentTopicMastery;
import com.ai.edu.domain.learning.model.valueobject.MasteryLevel;
import com.ai.edu.domain.learning.model.valueobject.TopicKey;
import com.ai.edu.domain.learning.repository.StudentQuestionRecordRepository;
import com.ai.edu.domain.learning.repository.StudentTopicMasteryRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 题型批量聚集（tasks 2.6，手动触发非定时，ADMIN 按钮）。
 *
 * <p>散名补归并：扫描题目表未归并（canonical_label IS NULL，PENDING）题型名 → 逐个过
 * {@link TopicLabelAggregationService}（归并/建锚）→ 更新题目表 canonical（幂等，仅更新 NULL）→
 * 从题目表重算掌握表（事实源聚合：按 student+canonical 累计平均生效分值，归并后的散题聚到 canonical）。
 *
 * <p>canonical 命名：建锚首见名兜底（aggregate 返回的规范名）；「最高频名 / LLM 归纳规范名」为扩展点
 * （tasks 2.6.2，本期无 LLM 归纳组件）。幂等：重复触发扫描不到已归并题型名，结果不变。
 */
@Slf4j
@Service
public class TopicClusterAppService {

    @Resource
    private StudentQuestionRecordRepository questionRecordRepository;

    @Resource
    private StudentTopicMasteryRepository studentTopicMasteryRepository;

    @Resource
    private TopicLabelAggregationService topicLabelAggregationService;

    /** 批量聚集：扫描未归并题型名 → 逐个聚集 → 更新 canonical → 重算掌握表。返回统计。 */
    public TopicClusterResult cluster() {
        List<String> pendingTopics = questionRecordRepository.findPendingTopicLabels();
        log.info("[topic-cluster] 批量聚集开始, 未归并题型名 {} 个", pendingTopics.size());
        int merged = 0;
        for (String topic : pendingTopics) {
            String canonical = topicLabelAggregationService.aggregate(topic, null);
            questionRecordRepository.updateCanonicalByTopic(topic, canonical);
            if (!canonical.equals(topic)) {
                merged++;
            }
        }
        recomputeMastery();
        return TopicClusterResult.of(pendingTopics.size(), merged);
    }

    /**
     * 重算掌握表（2.6.3）：题目表为事实源，按 (student, canonical) 分组累计平均生效分值，
     * upsert 掌握表行。PENDING（canonical 空）不参与（归属后再聚合）；无题目证据的历史行不删除。
     */
    private void recomputeMastery() {
        List<StudentQuestionRecord> all = questionRecordRepository.findAll();
        Map<StudentTopic, List<BigDecimal>> grouped = new LinkedHashMap<>();
        Map<StudentTopic, String> sources = new HashMap<>();
        for (StudentQuestionRecord r : all) {
            if (r.getCanonicalLabel() == null || r.getCanonicalLabel().isBlank()) {
                continue; // PENDING 不参与聚合（Decision 8）
            }
            if (r.getScore() == null) {
                continue; // analyze 贴题无信号（score=null），不参与聚合（区别于答错 0.00，SIG-007）
            }
            StudentTopic key = new StudentTopic(r.getStudentId(), r.getCanonicalLabel());
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r.getScore());
            sources.putIfAbsent(key, r.getSource());
        }
        for (Map.Entry<StudentTopic, List<BigDecimal>> e : grouped.entrySet()) {
            StudentTopic st = e.getKey();
            StudentTopicMastery mastery = StudentTopicMastery.restore(null, st.studentId(),
                    TopicKey.of(st.canonical()), st.canonical(),
                    MasteryLevel.notStarted(), null, null,
                    sources.getOrDefault(st, "ai"), 0, LocalDateTime.now());
            for (BigDecimal score : e.getValue()) {
                mastery.applyScore(score);
            }
            studentTopicMasteryRepository.upsert(mastery);
            log.info("[topic-cluster] 重算掌握表: student={}, canonical={}, trainCount={}, mastery={}",
                    st.studentId(), st.canonical(), mastery.getTrainCount(), mastery.getMasteryLevel().getValue());
        }
    }

    /** (studentId, canonical) 分组键。 */
    private record StudentTopic(Long studentId, String canonical) {
    }
}
