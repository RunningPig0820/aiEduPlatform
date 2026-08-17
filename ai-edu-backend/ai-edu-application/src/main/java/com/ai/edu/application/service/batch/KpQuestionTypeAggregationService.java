package com.ai.edu.application.service.batch;

import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.learning.model.entity.DerivedKpObs;
import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.model.entity.QuestionTypeAlias;
import com.ai.edu.domain.learning.model.entity.QuestionTypeKp;
import com.ai.edu.domain.learning.model.valueobject.QuestionTypeStatus;
import com.ai.edu.domain.learning.model.valueobject.TopicKpHint;
import com.ai.edu.domain.learning.repository.DerivedKpObsRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeAliasRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeKpRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import com.ai.edu.domain.learning.service.KpTopicAggregationPort;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 【离线处理 · 大数据归宿】知识点题型库聚合服务——从个体派生观测（obs）共现沉淀"知识点的题型"。
 *
 * <p>聚合：扫描已解析观测 → 按 topic_label 分组 → 达阈值建 CANDIDATE + 按 kp 拆分年级分布桶。
 * 变体别名合并：建新 CANDIDATE 前比对现有题型 kp_uri 分布重叠（≥ alias-overlap 阈值）→
 * 相似题型名（「鸡兔同笼」vs「鸡兔同笼问题」）收敛到 canonical + 别名表，聚合阈值不被变体拆分稀释；
 * canonical 分布按「主名 + 全部别名」union 观测重建（幂等）。
 * 业务隔离：只写 MySQL 派生层（题型库 + 别名），权威图谱零写入。
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
    @Resource
    private QuestionTypeAliasRepository questionTypeAliasRepository;
    @Resource
    private KgKnowledgePointRepository kgKnowledgePointRepository;
    @Resource
    private KpTopicAggregationPort topicAggregationPort;

    @Value("${ai-edu.kp.aggregation.candidate-students:3}")
    private int candidateStudents;
    @Value("${ai-edu.kp.aggregation.candidate-hits:5}")
    private int candidateHits;
    @Value("${ai-edu.kp.aggregation.stable-students:10}")
    private int stableStudents;
    @Value("${ai-edu.kp.aggregation.alias-overlap:0.7}")
    private double aliasOverlap;

    /**
     * 聚合：扫描已解析观测 → 按 canonical 分组（命中/相似变体/新建）→ union 重建分布。
     * 由 {@code KpBatchScheduler} 周期触发（凌晨 3:17），也可手动/大数据作业调用。
     */
    public void aggregate() {
        List<DerivedKpObs> obs = derivedKpObsRepository.findResolved();
        if (obs.isEmpty()) {
            return;
        }
        // 预载现有题型与 kp 签名（变体合并比对）；可变副本，本运行新建的 canonical 需追加
        List<QuestionType> existing = new ArrayList<>(questionTypeRepository.findAll());
        Map<Long, Set<String>> kpSignatures = new HashMap<>();
        for (QuestionTypeKp kp : questionTypeKpRepository.findAll()) {
            kpSignatures.computeIfAbsent(kp.getQuestionTypeId(), k -> new HashSet<>()).add(kp.getKpUri());
        }

        Map<String, List<DerivedKpObs>> byTopic = obs.stream()
                .collect(Collectors.groupingBy(DerivedKpObs::getTopicLabel));
        Map<Long, QuestionType> canonicalById = new LinkedHashMap<>();
        Map<Long, List<String>> labelsByCanonical = new LinkedHashMap<>();

        for (Map.Entry<String, List<DerivedKpObs>> entry : byTopic.entrySet()) {
            try {
                resolveCanonical(entry.getKey(), entry.getValue(), existing, kpSignatures,
                        canonicalById, labelsByCanonical);
            } catch (Exception e) {
                log.warn("聚合题型失败: topic={}", entry.getKey(), e);
            }
        }

        for (Map.Entry<Long, QuestionType> e : canonicalById.entrySet()) {
            try {
                rebuildCanonical(e.getValue(), labelsByCanonical.getOrDefault(e.getKey(), List.of()));
            } catch (Exception ex) {
                log.warn("重建题型分布失败: canonical={}", e.getKey(), ex);
            }
        }
        log.info("题型库聚合完成，扫描 {} 条观测，{} 个题型", obs.size(), canonicalById.size());
    }

    /** 单个题型 canonical 解析：① canonical/别名命中 → 现有；② 未命中 → kp 重叠相似变体；③ 无相似 → 新建。 */
    private void resolveCanonical(String topicLabel, List<DerivedKpObs> obs, List<QuestionType> existing,
                                  Map<Long, Set<String>> kpSignatures, Map<Long, QuestionType> canonicalById,
                                  Map<Long, List<String>> labelsByCanonical) {
        int distinctStudents = (int) obs.stream().map(DerivedKpObs::getStudentId).distinct().count();
        int totalHits = obs.stream().mapToInt(this::hitCount).sum();
        if (distinctStudents < candidateStudents || totalHits < candidateHits) {
            return;
        }
        Long promotedBy = obs.get(0).getStudentId();

        QuestionType canonical = questionTypeRepository.findByTopicLabelOrAlias(topicLabel).orElse(null);
        boolean isVariant = false;
        if (canonical == null) {
            canonical = findSimilarByKpOverlap(obs, existing, kpSignatures).orElse(null);
            isVariant = canonical != null;
        }
        if (canonical == null) {
            canonical = QuestionType.create(topicLabel, QuestionTypeStatus.CANDIDATE, promotedBy);
            canonical = questionTypeRepository.upsert(canonical);
            existing.add(canonical);
            kpSignatures.put(canonical.getId(), kpUris(obs));
        }
        if (isVariant) {
            questionTypeAliasRepository.upsert(QuestionTypeAlias.create(topicLabel, canonical.getId()));
            log.info("变体题型并入 canonical: {} → {} (id={})", topicLabel, canonical.getTopicLabel(), canonical.getId());
        }
        canonicalById.putIfAbsent(canonical.getId(), canonical);
        labelsByCanonical.computeIfAbsent(canonical.getId(), k -> new ArrayList<>()).add(topicLabel);
    }

    /** 变体合并：新桶 kp_uri 集合与现有题型 kp 签名重叠 ≥ 阈值（默认 0.7）→ 视同变体。 */
    private Optional<QuestionType> findSimilarByKpOverlap(List<DerivedKpObs> obs, List<QuestionType> existing,
                                                          Map<Long, Set<String>> kpSignatures) {
        Set<String> bucketKps = kpUris(obs);
        if (bucketKps.isEmpty()) {
            return Optional.empty();
        }
        QuestionType best = null;
        double bestOverlap = 0.0;
        for (QuestionType qt : existing) {
            Set<String> existingKps = kpSignatures.getOrDefault(qt.getId(), Set.of());
            if (existingKps.isEmpty()) {
                continue;
            }
            double overlap = intersectionSize(bucketKps, existingKps) / (double) bucketKps.size();
            if (overlap >= aliasOverlap && overlap > bestOverlap) {
                best = qt;
                bestOverlap = overlap;
            }
        }
        return Optional.ofNullable(best);
    }

    private int intersectionSize(Set<String> a, Set<String> b) {
        int n = 0;
        for (String s : a) {
            if (b.contains(s)) {
                n++;
            }
        }
        return n;
    }

    private Set<String> kpUris(List<DerivedKpObs> obs) {
        return obs.stream().map(DerivedKpObs::getKpUri).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /** 按 canonical 重建分布：union 主名 + 全部别名的 obs（幂等，重复聚合不放大统计）。 */
    private void rebuildCanonical(QuestionType canonical, List<String> thisRunLabels) {
        Set<String> allLabels = new LinkedHashSet<>(thisRunLabels);
        allLabels.add(canonical.getTopicLabel());
        for (QuestionTypeAlias alias : questionTypeAliasRepository.findByQuestionTypeId(canonical.getId())) {
            allLabels.add(alias.getAliasLabel());
        }
        List<DerivedKpObs> unionObs = derivedKpObsRepository.findResolvedByTopicLabels(allLabels);
        int distinctStudents = (int) unionObs.stream().map(DerivedKpObs::getStudentId).distinct().count();
        int totalHits = unionObs.stream().mapToInt(this::hitCount).sum();
        canonical.updateStats(distinctStudents, totalHits);
        canonical = questionTypeRepository.upsert(canonical);

        Map<String, List<DerivedKpObs>> byKp = unionObs.stream()
                .collect(Collectors.groupingBy(DerivedKpObs::getKpUri));
        // 多桶时 LLM 自动关联归纳 ratio（单桶/LLM 不可用降级纯计数）
        Map<String, Double> refined = refineDistribution(canonical.getTopicLabel(), byKp);
        for (Map.Entry<String, List<DerivedKpObs>> kpEntry : byKp.entrySet()) {
            double ratio = (refined != null && refined.containsKey(kpEntry.getKey()))
                    ? refined.get(kpEntry.getKey())
                    : countRatio(kpEntry.getValue(), totalHits);
            upsertBucket(canonical.getId(), kpEntry.getKey(), kpEntry.getValue(), ratio);
        }
    }

    /** LLM 自动关联：把共现桶组装成 hint 交 LLM 归纳归一化 ratio；单桶/失败返回 null。 */
    private Map<String, Double> refineDistribution(String topicLabel, Map<String, List<DerivedKpObs>> byKp) {
        if (byKp.size() <= 1) {
            return null;
        }
        List<TopicKpHint> hints = new ArrayList<>();
        for (Map.Entry<String, List<DerivedKpObs>> e : byKp.entrySet()) {
            String kpUri = e.getKey();
            String kpLabel = kgKnowledgePointRepository.findByUri(kpUri)
                    .map(KgKnowledgePoint::getLabel).orElse(null);
            hints.add(TopicKpHint.builder()
                    .kpUri(kpUri)
                    .kpLabel(kpLabel)
                    .hitCount(e.getValue().stream().mapToInt(this::hitCount).sum())
                    .gradeRange(computeGradeRange(e.getValue()))
                    .build());
        }
        return topicAggregationPort.refineDistribution(topicLabel, hints);
    }

    /** 纯计数 ratio（降级兜底）。 */
    private double countRatio(List<DerivedKpObs> obs, int totalHits) {
        if (totalHits <= 0) {
            return 0.0;
        }
        return (double) obs.stream().mapToInt(this::hitCount).sum() / totalHits;
    }

    /** 分布桶：按 kp 统计 hit_students/hit_count，ratio 由调用方给定（LLM 归纳或纯计数），grade_range 取学生年级 min-max。 */
    private void upsertBucket(Long questionTypeId, String kpUri, List<DerivedKpObs> obs, double ratio) {
        int kpStudents = (int) obs.stream().map(DerivedKpObs::getStudentId).distinct().count();
        int kpHits = obs.stream().mapToInt(this::hitCount).sum();
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
