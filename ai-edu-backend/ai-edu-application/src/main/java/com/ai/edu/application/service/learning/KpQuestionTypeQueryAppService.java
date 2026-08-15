package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.PageDTO;
import com.ai.edu.application.dto.learning.QuestionTypeKpDTO;
import com.ai.edu.application.dto.learning.QuestionTypePageItemDTO;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.model.entity.QuestionTypeKp;
import com.ai.edu.domain.learning.repository.QuestionTypeKpRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import jakarta.annotation.Resource;
import lombok.AccessLevel;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 题型库查询应用服务（题型分析页）：题型库分页 + 题型关联知识点。
 *
 * <p>题型库（t_kp_question_type / t_kp_question_type_kp）只存 kp_uri，不冗余知识点 name；
 * kpLabel 每次从 kg 镜像反查（权威标签唯一来源）。
 */
@Service
public class KpQuestionTypeQueryAppService {

    @Resource
    @Setter(AccessLevel.PACKAGE)
    private QuestionTypeRepository questionTypeRepository;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private QuestionTypeKpRepository questionTypeKpRepository;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private KgKnowledgePointRepository kgKnowledgePointRepository;

    /** 题型库分页。 */
    public PageDTO<QuestionTypePageItemDTO> page(int page, int size) {
        long total = questionTypeRepository.count();
        List<QuestionTypePageItemDTO> items = questionTypeRepository.findPage((page - 1) * size, size).stream()
                .map(qt -> QuestionTypePageItemDTO.builder()
                        .id(qt.getId())
                        .topicLabel(qt.getTopicLabel())
                        .status(qt.getStatus() == null ? null : qt.getStatus().name())
                        .hitCount(qt.getHitCount())
                        .build())
                .toList();
        return PageDTO.<QuestionTypePageItemDTO>builder()
                .items(items).total(total).page(page).size(size).build();
    }

    /** 题型关联知识点（kpLabel 从 kg 镜像反查）。 */
    public List<QuestionTypeKpDTO> listKnowledgePoints(Long questionTypeId) {
        questionTypeRepository.findById(questionTypeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "题型不存在"));
        List<QuestionTypeKp> kps = questionTypeKpRepository.findByQuestionTypeId(questionTypeId);
        if (kps.isEmpty()) {
            return List.of();
        }
        List<String> kpUris = kps.stream().map(QuestionTypeKp::getKpUri).distinct().toList();
        Map<String, String> labelByUri = kgKnowledgePointRepository.findByUris(kpUris).stream()
                .collect(Collectors.toMap(KgKnowledgePoint::getUri, KgKnowledgePoint::getLabel, (a, b) -> a));
        return kps.stream()
                .map(kp -> QuestionTypeKpDTO.builder()
                        .kpUri(kp.getKpUri())
                        .kpLabel(labelByUri.get(kp.getKpUri()))
                        .gradeRange(kp.getGradeRange())
                        .ratio(kp.getRatio())
                        .hitCount(kp.getHitCount())
                        .build())
                .toList();
    }
}
