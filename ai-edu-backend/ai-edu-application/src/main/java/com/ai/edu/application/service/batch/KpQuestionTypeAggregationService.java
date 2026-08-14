package com.ai.edu.application.service.batch;

import com.ai.edu.domain.learning.model.entity.DerivedKpObs;
import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.model.entity.QuestionTypeKp;
import com.ai.edu.domain.learning.model.valueobject.QuestionTypeStatus;
import com.ai.edu.domain.learning.repository.DerivedKpObsRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeKpRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 知识点题型库聚合服务——从个体派生观测（obs）共现沉淀"知识点的题型"。
 *
 * <p>聚合：扫描已解析观测 → 按 topic_label 分组 → 达阈值建 CANDIDATE + 按 kp 拆分年级分布桶。
 * 业务隔离：只写 MySQL 派生层（题型库），权威图谱零写入。
 *
 * <p><b>逻辑归宿：大数据平台（离线批处理）。</b>
 * 本逻辑本质是离线批处理（不要求实时、obs 无限长尾），正确归宿是大数据平台
 * （Spark/Flink 批处理作业）。当前项目为纯 Java DDD 后端、暂未接入大数据平台，
 * 故先以 {@code @Scheduled} 周期任务在后端过渡实现。未来接大数据平台时，应：
 * 用大数据作业读 {@code t_kp_derived_obs} 写 {@code t_kp_question_type(_kp)}，
 * 删除本类，数据表与在线解析管线②（读先验）保持不变。
 */
@Slf4j
@Service
public class KpQuestionTypeAggregationService {

    @Resource
    private DerivedKpObsRepository derivedKpObsRepository;
    @Resource
    private QuestionTypeRepository questionTypeRepository;
    @Resource
    private QuestionTypeKpRepository questionTypeKpRepository;

    @Value("${ai-edu.kp.aggregation.candidate-students:3}")
    private int candidateStudents;
    @Value("${ai-edu.kp.aggregation.candidate-hits:5}")
    private int candidateHits;
    @Value("${ai-edu.kp.aggregation.stable-students:10}")
    private int stableStudents;

    /**
     * 聚合任务（周期）：扫描已解析观测 → 达阈值建 CANDIDATE + 分布桶。
     * 凌晨 3:17 触发（避开整点）。
     */
    @Scheduled(cron = "0 17 3 * * ?")
    public void aggregate() {
        List<DerivedKpObs> obs = derivedKpObsRepository.findResolved();
        if (obs.isEmpty()) {
            return;
        }
        Map<String, List<DerivedKpObs>> byTopic = obs.stream()
                .collect(Collectors.groupingBy(DerivedKpObs::getTopicLabel));
        for (Map.Entry<String, List<DerivedKpObs>> entry : byTopic.entrySet()) {
            try {
                aggregateTopic(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                log.warn("聚合题型失败: topic={}", entry.getKey(), e);
            }
        }
        log.info("题型库聚合完成，扫描 {} 条观测，{} 个题型", obs.size(), byTopic.size());
    }

    /** 单个题型聚合：达阈值建 CANDIDATE + 按 kp 拆分分布桶。 */
    private void aggregateTopic(String topicLabel, List<DerivedKpObs> obs) {
        int distinctStudents = (int) obs.stream().map(DerivedKpObs::getStudentId).distinct().count();
        int totalHits = obs.stream().mapToInt(this::hitCount).sum();
        if (distinctStudents < candidateStudents || totalHits < candidateHits) {
            return;
        }
        Long promotedBy = obs.get(0).getStudentId();
        QuestionType qt = questionTypeRepository.findByTopicLabel(topicLabel)
                .orElseGet(() -> QuestionType.create(topicLabel, QuestionTypeStatus.CANDIDATE, promotedBy));
        qt.updateStats(distinctStudents, totalHits);
        qt = questionTypeRepository.upsert(qt);

        Map<String, List<DerivedKpObs>> byKp = obs.stream()
                .collect(Collectors.groupingBy(DerivedKpObs::getKpUri));
        for (Map.Entry<String, List<DerivedKpObs>> kpEntry : byKp.entrySet()) {
            upsertBucket(qt.getId(), kpEntry.getKey(), kpEntry.getValue(), totalHits);
        }
    }

    /** 分布桶：按 kp 统计 hit_students/hit_count/ratio，grade_range 取学生年级 min-max。 */
    private void upsertBucket(Long questionTypeId, String kpUri, List<DerivedKpObs> obs, int totalHits) {
        int kpStudents = (int) obs.stream().map(DerivedKpObs::getStudentId).distinct().count();
        int kpHits = obs.stream().mapToInt(this::hitCount).sum();
        double ratio = totalHits > 0 ? (double) kpHits / totalHits : 0.0;
        String gradeRange = computeGradeRange(obs);
        QuestionTypeKp kp = QuestionTypeKp.create(questionTypeId, kpUri, gradeRange);
        kp.updateStats(kpStudents, kpHits, ratio, gradeRange);
        questionTypeKpRepository.upsert(kp);
    }

    private int hitCount(DerivedKpObs obs) {
        return obs.getOccurrenceCount() == null ? 1 : obs.getOccurrenceCount();
    }

    /** 年级段：该 kp 命中的学生年级 min-max（如 4-6）；无年级锚返回 null。 */
    private String computeGradeRange(List<DerivedKpObs> obs) {
        List<Integer> grades = obs.stream()
                .map(DerivedKpObs::getStudentGrade)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        if (grades.isEmpty()) {
            return null;
        }
        int min = grades.get(0);
        int max = grades.get(grades.size() - 1);
        return min == max ? String.valueOf(min) : min + "-" + max;
    }

    /**
     * 升 STABLE（人工审核通过后调用）：去重学生数须 ≥ stable-students 阈值。
     *
     * @param topicLabel 题型名
     * @param definition 可选定义（可空）
     */
    public void promoteToStable(String topicLabel, String definition) {
        QuestionType qt = questionTypeRepository.findByTopicLabel(topicLabel)
                .orElseThrow(() -> new IllegalArgumentException("题型不存在: " + topicLabel));
        int students = qt.getHitStudents() == null ? 0 : qt.getHitStudents();
        if (students < stableStudents) {
            throw new IllegalStateException(
                    "未达升 STABLE 阈值（≥" + stableStudents + " 学生），当前 " + students);
        }
        qt.promoteToStable(definition);
        questionTypeRepository.upsert(qt);
        log.info("题型升 STABLE: topic={}", topicLabel);
    }
}
