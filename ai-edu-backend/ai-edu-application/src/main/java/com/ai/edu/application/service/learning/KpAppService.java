package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.ConfirmKpAliasDTO;
import com.ai.edu.application.dto.learning.KpResolveDTO;
import com.ai.edu.application.dto.learning.PendingKpAliasDTO;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.learning.model.entity.DerivedKpObs;
import com.ai.edu.domain.learning.model.valueobject.DerivedKpStatus;
import com.ai.edu.domain.learning.model.valueobject.KpResolution;
import com.ai.edu.domain.learning.repository.DerivedKpObsRepository;
import com.ai.edu.domain.learning.service.TutoringKpResolver;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识点解析与挂起审核应用服务（在线）。
 *
 * <p>resolve：复用答疑内嵌解析管线（写 obs）。挂起审核：列出 PENDING/HUMAN_REVIEW 观测，
 * 人工确认归属（source=curated），题型库统计回流由周期聚合任务处理。
 */
@Service
public class KpAppService {

    @Resource
    private TutoringKpResolver kpResolver;
    @Resource
    private DerivedKpObsRepository derivedKpObsRepository;
    @Resource
    private KgKnowledgePointRepository kgKnowledgePointRepository;

    /** 题型解析（复用解析管线，写观测）。 */
    public KpResolveDTO resolve(String label, Long studentId) {
        KpResolution r = kpResolver.resolve(label, studentId);
        return KpResolveDTO.builder()
                .label(r.getLabel())
                .uri(r.getUri())
                .kpLabel(r.getKpLabel())
                .confidence(r.getConfidence())
                .status(r.getStatus())
                .candidates(r.getCandidates())
                .build();
    }

    /** 列出挂起观测（PENDING + HUMAN_REVIEW），供管理端审核。 */
    public List<PendingKpAliasDTO> listPending() {
        List<DerivedKpObs> obs = new ArrayList<>();
        obs.addAll(derivedKpObsRepository.findByStatus(DerivedKpStatus.PENDING));
        obs.addAll(derivedKpObsRepository.findByStatus(DerivedKpStatus.HUMAN_REVIEW));
        return obs.stream().map(this::toPendingDTO).toList();
    }

    /** 列出某学生的疑似观测（PENDING + WEAK），供学生端"待确认清单"。 */
    public List<PendingKpAliasDTO> listPendingByStudent(Long studentId) {
        return derivedKpObsRepository.findByStudentId(studentId).stream()
                .filter(o -> o.getStatus() == DerivedKpStatus.PENDING || o.getStatus() == DerivedKpStatus.WEAK)
                .map(this::toPendingDTO)
                .toList();
    }

    /** 人工确认挂起观测归属知识点 URI。 */
    public ConfirmKpAliasDTO confirm(Long id, String kpUri) {
        if (kpUri == null || kpUri.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "kp_uri 不能为空");
        }
        if (kgKnowledgePointRepository.findByUri(kpUri).isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "kp_uri 不存在于 kg 镜像");
        }
        int updated = derivedKpObsRepository.confirm(id, kpUri);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.KP_OBS_NOT_FOUND, "派生观测不存在或已处理");
        }
        return ConfirmKpAliasDTO.builder().updated(true).status("RESOLVED").build();
    }

    /** 学生澄清投票：学生选择归属概念，落 source=student_vote 观测；候选无法解析到知识点时报错（不静默）。 */
    public void vote(String topicLabel, Long studentId, String selectedLabel) {
        if (!kpResolver.recordStudentVote(topicLabel, studentId, selectedLabel)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "候选知识点不存在，无法投票");
        }
    }

    private PendingKpAliasDTO toPendingDTO(DerivedKpObs o) {
        String kpLabel = o.getKpUri() == null ? null
                : kgKnowledgePointRepository.findByUri(o.getKpUri())
                        .map(KgKnowledgePoint::getLabel).orElse(null);
        return PendingKpAliasDTO.builder()
                .id(o.getId())
                .topicLabel(o.getTopicLabel())
                .studentId(o.getStudentId())
                .studentGrade(o.getStudentGrade())
                .confidence(o.getConfidence())
                .status(o.getStatus() == null ? null : o.getStatus().name())
                .kpUri(o.getKpUri())
                .kpLabel(kpLabel)
                .firstSeenAt(o.getFirstSeenAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
