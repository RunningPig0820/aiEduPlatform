package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.KpCoverageDTO;
import com.ai.edu.application.dto.learning.KpCoverageItemDTO;
import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.model.valueobject.KgKpPlacement;
import com.ai.edu.domain.edukg.model.valueobject.KgStageEnum;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.learning.model.entity.DerivedKpObs;
import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.model.entity.QuestionTypeKp;
import com.ai.edu.domain.learning.model.entity.StudentKpMastery;
import com.ai.edu.domain.learning.model.entity.StudentTopicMastery;
import com.ai.edu.domain.learning.repository.DerivedKpObsRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeKpRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import com.ai.edu.domain.learning.repository.StudentKpMasteryRepository;
import com.ai.edu.domain.learning.repository.StudentTopicMasteryRepository;
import com.ai.edu.domain.learning.service.TopicKeyNormalizer;
import jakarta.annotation.Resource;
import lombok.AccessLevel;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 知识点派生覆盖度应用服务（掌握度主体翻转后新增）。
 *
 * <p>知识点掌握度不再直接观测，改为从题型掌握度派生：
 * {@code coverage(kp) = clamp(Σ_{题型→kp} (题型掌握度 × ratio), 0, 75)}。
 *
 * <p>ratio 来源：优先 {@code t_kp_question_type_kp.ratio}（聚合后跨学生分布）；
 * 题型未聚合时用该生 {@code t_kp_derived_obs} 单观测（ratio 隐式 1）；
 * 无题型映射的知识点回退旧 {@code t_student_kp_mastery}（过渡期）。
 */
@Service
public class KpCoverageAppService {

    @Resource
    @Setter(AccessLevel.PACKAGE)
    private StudentTopicMasteryRepository topicMasteryRepository;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private DerivedKpObsRepository derivedKpObsRepository;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private QuestionTypeRepository questionTypeRepository;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private QuestionTypeKpRepository questionTypeKpRepository;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private StudentKpMasteryRepository kpMasteryRepository;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private KgKnowledgePointRepository kgKnowledgePointRepository;

    /** 计算某学生知识点派生覆盖度。 */
    public KpCoverageDTO getKpCoverage(Long studentId) {
        List<StudentTopicMastery> topics = topicMasteryRepository.findByStudentId(studentId);
        List<DerivedKpObs> obsList = derivedKpObsRepository.findByStudentId(studentId);

        // 该生观测：kp_uri → 最高置信度；归一化 topic_key → 该生已解析观测（单观测 fallback）
        Map<String, Integer> confidenceByKp = obsList.stream()
                .filter(o -> o.getKpUri() != null && o.getConfidence() != null)
                .collect(Collectors.toMap(DerivedKpObs::getKpUri, DerivedKpObs::getConfidence, Math::max));
        Map<String, List<DerivedKpObs>> obsByTopic = obsList.stream()
                .filter(o -> o.getKpUri() != null && o.getTopicLabel() != null)
                .collect(Collectors.groupingBy(o -> TopicKeyNormalizer.normalize(o.getTopicLabel())));

        // 题型掌握度 × ratio → 知识点覆盖度
        Map<String, Double> coverageByKp = new HashMap<>();
        for (StudentTopicMastery t : topics) {
            int level = t.getMasteryLevel() == null ? 0 : t.getMasteryLevel().getValue();
            for (KpBucket bucket : resolveBuckets(t.getTopicLabel(), t.getTopicKey().getValue(), obsByTopic)) {
                coverageByKp.merge(bucket.kpUri, level * bucket.ratio, Double::sum);
            }
        }

        // 过渡期回退：无题型映射的知识点取旧 KP 掌握度
        for (StudentKpMastery m : kpMasteryRepository.findByStudentId(studentId)) {
            if (m.getKpKey() == null) {
                continue;
            }
            String uri = m.getKpKey().getValue();
            coverageByKp.putIfAbsent(uri, m.getMasteryLevel() == null ? 0.0 : (double) m.getMasteryLevel().getValue());
        }

        // 组装：kpLabel / stage / chapterLabel / sectionLabel 批量反查 kg 镜像
        List<String> uris = new ArrayList<>(coverageByKp.keySet());
        Map<String, String> labelByUri = kgKnowledgePointRepository.findByUris(uris).stream()
                .collect(Collectors.toMap(KgKnowledgePoint::getUri, KgKnowledgePoint::getLabel, (a, b) -> a));
        Map<String, KgKpPlacement> placementByKp = kgKnowledgePointRepository.findPlacementByUris(uris).stream()
                .collect(Collectors.toMap(KgKpPlacement::getKpUri, p -> p, (a, b) -> a));

        List<KpCoverageItemDTO> items = new ArrayList<>();
        for (Map.Entry<String, Double> e : coverageByKp.entrySet()) {
            double cov = clampCoverage(e.getValue());
            KgKpPlacement placement = placementByKp.get(e.getKey());
            items.add(KpCoverageItemDTO.builder()
                    .kpUri(e.getKey())
                    .kpLabel(labelByUri.get(e.getKey()))
                    .coverage((int) Math.round(cov))
                    .masteryLevel(discretize(cov))
                    .status("RESOLVED")
                    .confidence(confidenceByKp.get(e.getKey()))
                    .stage(toStageCode(placement == null ? null : placement.getStage()))
                    .chapterLabel(placement == null ? null : placement.getChapterLabel())
                    .sectionLabel(placement == null ? null : placement.getSectionLabel())
                    .build());
        }
        items.sort(Comparator.comparing(KpCoverageItemDTO::getKpUri, Comparator.nullsLast(String::compareTo)));
        return KpCoverageDTO.builder().studentId(studentId).items(items).build();
    }

    /** 求某题型的知识点分布桶：优先聚合题型库 ratio，未聚合回退该生单观测（ratio=1）。 */
    private List<KpBucket> resolveBuckets(String topicLabel, String topicKey,
                                          Map<String, List<DerivedKpObs>> obsByTopic) {
        Optional<QuestionType> qt = topicLabel == null ? Optional.empty() : questionTypeRepository.findByTopicLabel(topicLabel);
        if (qt.isPresent()) {
            List<QuestionTypeKp> kps = questionTypeKpRepository.findByQuestionTypeId(qt.get().getId());
            if (!kps.isEmpty()) {
                return kps.stream()
                        .filter(kp -> kp.getKpUri() != null)
                        .map(kp -> new KpBucket(kp.getKpUri(), kp.getRatio() == null ? 0.0 : kp.getRatio()))
                        .toList();
            }
        }
        return obsByTopic.getOrDefault(topicKey, List.of()).stream()
                .map(o -> new KpBucket(o.getKpUri(), 1.0))
                .toList();
    }

    /** 覆盖度封顶 0-75（题型四档顶 75，避免多题型叠加溢出）。 */
    private static double clampCoverage(double value) {
        return Math.max(0.0, Math.min(75.0, value));
    }

    /** 连续覆盖度 → 离散四档：≥75→75 / ≥50→50 / ≥25→25 / 否则 0。 */
    private static int discretize(double coverage) {
        if (coverage >= 75) {
            return 75;
        }
        if (coverage >= 50) {
            return 50;
        }
        if (coverage >= 25) {
            return 25;
        }
        return 0;
    }

    /** 中文 stage label → code（未知原样返回，null 返回 null）。 */
    private static String toStageCode(String label) {
        KgStageEnum e = KgStageEnum.fromLabel(label);
        return e == null ? label : e.getCode();
    }

    /** 题型→知识点 分布桶（ratio 已归一化）。 */
    private record KpBucket(String kpUri, double ratio) {
    }
}
