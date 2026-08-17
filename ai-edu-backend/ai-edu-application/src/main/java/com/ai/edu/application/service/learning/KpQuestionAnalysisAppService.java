package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.QuestionAnalysisDTO;
import com.ai.edu.application.dto.learning.QuestionAnalysisKpDTO;
import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.model.entity.QuestionTypeKp;
import com.ai.edu.domain.learning.repository.DerivedKpObsRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeKpRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import com.ai.edu.domain.learning.service.QuestionUnderstandingPort;
import com.ai.edu.domain.learning.service.TutoringKpResolver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 单题分析应用服务（POST /api/kp/analyze-question）——题目文本 → 题型名 → 关联知识点清单。
 *
 * <p>本期范围（前端降级 2026-08-17）：
 * ① 题型库 canonical/别名命中 → 权威分布（数据驱动）；
 * ② 题库 miss → PENDING + 挂起 PENDING obs（空可接受，前端 keyword 搜索确认兜底）。
 * <p>「题库和知识点」池约束选择（D8）已抽到 {@link KpPoolAssociateService}，
 * 本期 analyze 未接线，待独立迭代启用（题库 miss → 学段池 → LLM 从池选 top-N → 恒非空）。
 */
@Slf4j
@Service
public class KpQuestionAnalysisAppService {

    @Resource
    private QuestionUnderstandingPort questionUnderstandingPort;
    @Resource
    private TutoringKpResolver tutoringKpResolver;
    @Resource
    private QuestionTypeRepository questionTypeRepository;
    @Resource
    private QuestionTypeKpRepository questionTypeKpRepository;
    @Resource
    private KgKnowledgePointRepository kgKnowledgePointRepository;
    @Resource
    private DerivedKpObsRepository derivedKpObsRepository;

    public QuestionAnalysisDTO analyze(String text, Long studentId) {
        Integer grade = tutoringKpResolver.resolveStudentGrade(studentId);
        List<String> topics = questionUnderstandingPort.understand(text, grade);

        // ① 题型库 canonical/别名命中 → 权威分布（数据驱动，顺序无关）
        if (!topics.isEmpty()) {
            for (String topic : topics) {
                Optional<QuestionType> qt = questionTypeRepository.findByTopicLabelOrAlias(topic);
                if (qt.isPresent()) {
                    return catalogResult(qt.get());
                }
            }
        }

        // ② 题库 miss → PENDING + 挂起（空可接受；池约束选择待「题库和知识点」迭代接线）
        if (studentId != null && !topics.isEmpty()) {
            derivedKpObsRepository.upsertPendingIfAbsent(studentId, topics.get(0), grade);
        }
        return QuestionAnalysisDTO.pending(topics.isEmpty() ? null : topics.get(0), List.of());
    }

    /** 题型库命中：全部关联知识点分布（kpLabel 从镜像反查，gradeRange/ratio 透传），confidence=最大占比。 */
    private QuestionAnalysisDTO catalogResult(QuestionType qt) {
        List<QuestionTypeKp> kps = questionTypeKpRepository.findByQuestionTypeId(qt.getId());
        double maxRatio = kps.stream()
                .mapToDouble(k -> k.getRatio() == null ? 0.0 : k.getRatio())
                .max().orElse(0.0);
        List<QuestionAnalysisKpDTO> items = kps.stream()
                .map(k -> QuestionAnalysisKpDTO.builder()
                        .kpUri(k.getKpUri())
                        .kpLabel(kpLabelOf(k.getKpUri()))
                        .gradeRange(k.getGradeRange())
                        .ratio(k.getRatio())
                        .build())
                .toList();
        return QuestionAnalysisDTO.resolved(qt.getTopicLabel(), (int) Math.round(maxRatio * 100), items);
    }

    private String kpLabelOf(String kpUri) {
        return kgKnowledgePointRepository.findByUri(kpUri)
                .map(KgKnowledgePoint::getLabel)
                .orElse(null);
    }
}
