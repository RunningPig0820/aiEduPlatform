package com.ai.edu.infrastructure.persistence.learning.po;

import com.ai.edu.domain.learning.model.entity.QuestionTypeAlias;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 题型库变体别名持久化对象（表：t_kp_question_type_alias，ai_edu_learning 库）。
 */
@TableName("t_kp_question_type_alias")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionTypeAliasPo {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("alias_label")
    private String aliasLabel;

    @TableField("question_type_id")
    private Long questionTypeId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("created_by")
    private Long createdBy = 0L;

    @TableField("modified_by")
    private Long modifiedBy = 0L;

    @TableField("is_deleted")
    private Boolean deleted = false;

    public static QuestionTypeAliasPo from(QuestionTypeAlias entity) {
        if (entity == null) {
            return null;
        }
        QuestionTypeAliasPo po = new QuestionTypeAliasPo();
        po.id = entity.getId();
        po.aliasLabel = entity.getAliasLabel();
        po.questionTypeId = entity.getQuestionTypeId();
        po.createdAt = entity.getCreatedAt();
        return po;
    }

    public QuestionTypeAlias toEntity() {
        return QuestionTypeAlias.restore(this.id, this.aliasLabel, this.questionTypeId, this.createdAt);
    }
}
