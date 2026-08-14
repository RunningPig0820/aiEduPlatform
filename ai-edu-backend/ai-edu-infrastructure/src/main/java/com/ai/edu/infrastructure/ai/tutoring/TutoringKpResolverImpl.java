package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.learning.model.entity.DerivedKpObs;
import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.model.entity.QuestionTypeKp;
import com.ai.edu.domain.learning.model.valueobject.DerivedKpSource;
import com.ai.edu.domain.learning.model.valueobject.DerivedKpStatus;
import com.ai.edu.domain.learning.model.valueobject.KpResolution;
import com.ai.edu.domain.learning.repository.DerivedKpObsRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeKpRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import com.ai.edu.domain.learning.service.KpDisambiguationPort;
import com.ai.edu.domain.learning.service.TutoringKpResolver;
import com.ai.edu.domain.organization.model.entity.StudentClass;
import com.ai.edu.domain.organization.repository.ClassRepository;
import com.ai.edu.domain.organization.repository.StudentClassRepository;
import com.ai.edu.domain.shared.valueobject.UserId;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 知识点 label → TextbookKP URI 解析实现（派生层解析管线）。
 *
 * <p>管线：① kg-sync 镜像精确/LIKE → ② 题型库年级匹配 → ③ LLM 消歧 → ④ 低置信 PENDING（含澄清候选）。
 * 每次解析写个体派生观测（{@code t_kp_derived_obs}）：命中 source=MIRROR/CATALOG、冷启动 LLM 标 WEAK、
 * 挂起标 PENDING；权威图谱（Neo4j + 镜像）只读。解析失败不阻断答疑主流程。
 *
 * <p>跨域访问通过 domain 仓储接口（{@link KgKnowledgePointRepository} 等）而非 mapper，符合 DDD 分层。
 * LLM 消歧委托 {@link KpLlmDisambiguator}（与维护闭环共用，DRY）。
 */
@Slf4j
@Service
public class TutoringKpResolverImpl implements TutoringKpResolver {

    /** 学生澄清投票的中等置信（软信号，≥3 人去重一致才进候选）。 */
    private static final int STUDENT_VOTE_CONFIDENCE = 60;

    @Resource
    private KgKnowledgePointRepository kgKnowledgePointRepository;
    @Resource
    private QuestionTypeRepository questionTypeRepository;
    @Resource
    private QuestionTypeKpRepository questionTypeKpRepository;
    @Resource
    private StudentClassRepository studentClassRepository;
    @Resource
    private ClassRepository classRepository;
    @Resource
    private DerivedKpObsRepository derivedKpObsRepository;
    @Resource
    private KpDisambiguationPort kpDisambiguationPort;

    @Value("${ai-edu.kp.confidence-threshold:60}")
    private int confidenceThreshold;

    @Override
    public KpResolution resolve(String label, Long studentId) {
        if (label == null || label.isBlank()) {
            return KpResolution.pending(label);
        }
        Integer grade = resolveGrade(studentId);

        // ① 镜像精确 / LIKE（确定性规则，0 依赖 LLM）
        Optional<KgKnowledgePoint> exact = kgKnowledgePointRepository.findByLabel(label);
        if (exact.isPresent() && exact.get().getUri() != null) {
            writeObs(studentId, label, exact.get().getUri(), grade, 100, DerivedKpSource.MIRROR, DerivedKpStatus.RESOLVED);
            return KpResolution.resolved(label, exact.get().getUri(), exact.get().getLabel(), 100);
        }
        Optional<KgKnowledgePoint> like = kgKnowledgePointRepository.findByLabelLike(label);
        if (like.isPresent() && like.get().getUri() != null) {
            log.debug("知识点 label 模糊命中: {} -> {}", label, like.get().getUri());
            writeObs(studentId, label, like.get().getUri(), grade, 80, DerivedKpSource.MIRROR, DerivedKpStatus.RESOLVED);
            return KpResolution.resolved(label, like.get().getUri(), like.get().getLabel(), 80);
        }

        // ② 题型库年级匹配（统计先验，数据驱动）
        KpResolution catalog = resolveByCatalog(label, grade);
        if (catalog != null) {
            writeObs(studentId, label, catalog.getUri(), grade, catalog.getConfidence(),
                    DerivedKpSource.CATALOG, DerivedKpStatus.RESOLVED);
            return catalog;
        }

        // ③ LLM 消歧（主裁判，冷启动种子）
        KpResolution llm = kpDisambiguationPort.disambiguate(label, grade);
        if (llm != null && llm.getConfidence() >= confidenceThreshold) {
            // 冷启动弱化：题型库无先验 → 标 WEAK，不直接点亮，待第二独立信号转 RESOLVED
            writeObs(studentId, label, llm.getUri(), grade, llm.getConfidence(),
                    DerivedKpSource.LLM, DerivedKpStatus.WEAK);
            return llm;
        }

        // ④ 低置信/歧义 → PENDING（含澄清候选，供学生"你想学哪个"）
        writeObs(studentId, label, null, grade, 0, DerivedKpSource.LLM, DerivedKpStatus.PENDING);
        List<KgKnowledgePoint> candidates = kgKnowledgePointRepository.findByLabelLikeList(label);
        List<String> candidateLabels = candidates.stream()
                .map(KgKnowledgePoint::getLabel)
                .distinct()
                .toList();
        log.warn("知识点 label 解析失败（挂起，不点亮）: {}", label);
        return KpResolution.pending(label, candidateLabels);
    }

    @Override
    public void recordStudentVote(String topicLabel, Long studentId, String selectedLabel) {
        if (studentId == null || selectedLabel == null || selectedLabel.isBlank()) {
            return;
        }
        Optional<KgKnowledgePoint> kp = kgKnowledgePointRepository.findByLabel(selectedLabel);
        if (kp.isEmpty() || kp.get().getUri() == null) {
            log.warn("学生澄清候选不在镜像，忽略: {}", selectedLabel);
            return;
        }
        Integer grade = resolveGrade(studentId);
        DerivedKpObs obs = DerivedKpObs.create(studentId, topicLabel, kp.get().getUri(), grade,
                STUDENT_VOTE_CONFIDENCE, DerivedKpSource.STUDENT_VOTE, DerivedKpStatus.RESOLVED);
        derivedKpObsRepository.upsert(obs);
        log.info("学生澄清投票落观测: studentId={}, topic={}, kp={}", studentId, topicLabel, kp.get().getUri());
    }

    /** 写个体派生观测；studentId 为空（纯解析，resolveLabelToUri 兼容）时不写。 */
    private void writeObs(Long studentId, String label, String uri, Integer grade, int confidence,
                          DerivedKpSource source, DerivedKpStatus status) {
        if (studentId == null) {
            return;
        }
        DerivedKpObs obs = DerivedKpObs.create(studentId, label, uri, grade, confidence, source, status);
        derivedKpObsRepository.upsert(obs);
    }

    /** 学生 → 班级 → 年级（组织系统）；不可得返回 null，降级纯 LLM 消歧。 */
    private Integer resolveGrade(Long studentId) {
        if (studentId == null) {
            return null;
        }
        try {
            return studentClassRepository.findActiveByStudentId(UserId.of(studentId))
                    .map(StudentClass::getClassId)
                    .flatMap(classRepository::findById)
                    .map(cls -> cls.getGrade().getValue())
                    .orElse(null);
        } catch (Exception e) {
            log.debug("查学生年级失败，降级纯 LLM 消歧: studentId={}", studentId, e);
            return null;
        }
    }

    /** ② 题型库年级匹配：同题型按学生年级取占比最高 kp。 */
    private KpResolution resolveByCatalog(String label, Integer grade) {
        Optional<QuestionType> qt = questionTypeRepository.findByTopicLabel(label);
        if (qt.isEmpty()) {
            return null;
        }
        List<QuestionTypeKp> kps = questionTypeKpRepository.findByQuestionTypeId(qt.get().getId());
        if (kps.isEmpty()) {
            return null;
        }
        QuestionTypeKp best = selectByGrade(kps, grade);
        if (best == null) {
            return null;
        }
        String kpLabel = kgKnowledgePointRepository.findByUri(best.getKpUri())
                .map(KgKnowledgePoint::getLabel)
                .orElse(null);
        int confidence = (int) Math.round(best.getRatio() * 100);
        return KpResolution.resolved(label, best.getKpUri(), kpLabel, confidence);
    }

    private QuestionTypeKp selectByGrade(List<QuestionTypeKp> kps, Integer grade) {
        if (grade == null) {
            return kps.stream().max(Comparator.comparing(QuestionTypeKp::getRatio)).orElse(null);
        }
        return kps.stream()
                .filter(kp -> gradeInRange(kp.getGradeRange(), grade))
                .max(Comparator.comparing(QuestionTypeKp::getRatio))
                .orElseGet(() -> kps.stream().max(Comparator.comparing(QuestionTypeKp::getRatio)).orElse(null));
    }

    private boolean gradeInRange(String range, Integer grade) {
        if (range == null || range.isBlank()) {
            return false;
        }
        try {
            String[] parts = range.split("-");
            int lo = Integer.parseInt(parts[0].trim());
            int hi = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : lo;
            return grade >= lo && grade <= hi;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
