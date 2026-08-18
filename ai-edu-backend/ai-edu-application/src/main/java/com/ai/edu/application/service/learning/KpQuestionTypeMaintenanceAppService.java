package com.ai.edu.application.service.learning;

import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.model.entity.QuestionTypeAlias;
import com.ai.edu.domain.learning.model.entity.QuestionTypeKp;
import com.ai.edu.domain.learning.model.valueobject.QuestionTypeStatus;
import com.ai.edu.domain.learning.repository.QuestionTypeAliasRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeKpRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import jakarta.annotation.Resource;
import lombok.AccessLevel;
import lombok.Setter;
import org.springframework.stereotype.Service;

/**
 * 题型↔知识点维护应用服务（域 B 独立化 Decision 10 / tasks 2.0.5）。
 *
 * <p><b>独立逻辑</b>：题型库（t_kp_question_type / t_kp_question_type_kp / 别名表）由本服务
 * ADMIN 手动维护——替代「obs 共现 → LLM 归纳 → 分布桶」的自动涌现链路（聚合/挂起/澄清批处理停用）。
 * 所有入口（analyze-question / 答疑）只读查表，命中即返回权威分布、未命中返回「仅题型」。
 *
 * <p>演示用法：手动配「鸡兔同笼 → 鸡兔同笼问题(ratio 0.6) / 假设法(ratio 0.4)」→ 入口命中题型库即返回关联知识点。
 */
@Service
public class KpQuestionTypeMaintenanceAppService {

    @Resource
    @Setter(AccessLevel.PACKAGE)
    private QuestionTypeRepository questionTypeRepository;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private QuestionTypeKpRepository questionTypeKpRepository;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private QuestionTypeAliasRepository questionTypeAliasRepository;

    /** 建/更新题型（topic_label 唯一，幂等 UPSERT）；已存在直接返回。 */
    public QuestionType upsertType(String topicLabel) {
        if (topicLabel == null || topicLabel.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "topicLabel 不能为空");
        }
        return questionTypeRepository.findByTopicLabel(topicLabel)
                .orElseGet(() -> questionTypeRepository.upsert(
                        QuestionType.create(topicLabel, QuestionTypeStatus.CANDIDATE, 0L)));
    }

    /** 升 STABLE（手动审核，替代聚合阈值判断）。 */
    public QuestionType promote(Long questionTypeId, String definition) {
        QuestionType qt = requireType(questionTypeId);
        qt.promoteToStable(definition);
        return questionTypeRepository.upsert(qt);
    }

    /** 绑知识点分布桶（question_type_id + kp_uri 唯一，UPSERT 幂等；ratio 0-1 校验）。 */
    public QuestionTypeKp bindKp(Long questionTypeId, String kpUri, double ratio, String gradeRange) {
        requireType(questionTypeId);
        if (kpUri == null || kpUri.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "kpUri 不能为空");
        }
        if (ratio < 0 || ratio > 1) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "ratio 须在 0-1 之间");
        }
        QuestionTypeKp kp = QuestionTypeKp.create(questionTypeId, kpUri, gradeRange);
        kp.updateStats(0, 0, ratio, gradeRange);
        return questionTypeKpRepository.upsert(kp);
    }

    /** 加变体别名（alias_label 唯一，UPSERT 幂等）——变体名 → canonical 题型。 */
    public QuestionTypeAlias addAlias(Long questionTypeId, String aliasLabel) {
        requireType(questionTypeId);
        if (aliasLabel == null || aliasLabel.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "aliasLabel 不能为空");
        }
        return questionTypeAliasRepository.upsert(QuestionTypeAlias.create(aliasLabel, questionTypeId));
    }

    private QuestionType requireType(Long questionTypeId) {
        if (questionTypeId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "questionTypeId 不能为空");
        }
        return questionTypeRepository.findById(questionTypeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "题型不存在: " + questionTypeId));
    }
}
