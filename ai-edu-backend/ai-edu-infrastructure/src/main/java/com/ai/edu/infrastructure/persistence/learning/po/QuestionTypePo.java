package com.ai.edu.infrastructure.persistence.learning.po;

import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.model.valueobject.QuestionTypeStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聚合题型库主表持久化对象（表：t_kp_question_type，ai_edu_learning 库）。
 */
@TableName("t_kp_question_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionTypePo {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("topic_label")
    private String topicLabel;

    @TableField("status")
    private String status;

    @TableField("definition")
    private String definition;

    @TableField("hit_students")
    private Integer hitStudents;

    @TableField("hit_count")
    private Integer hitCount;

    @TableField("promoted_by")
    private Long promotedBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("created_by")
    private Long createdBy = 0L;

    @TableField("modified_by")
    private Long modifiedBy = 0L;

    @TableField("is_deleted")
    private Boolean deleted = false;

    public static QuestionTypePo from(QuestionType entity) {
        if (entity == null) {
            return null;
        }
        QuestionTypePo po = new QuestionTypePo();
        po.id = entity.getId();
        po.topicLabel = entity.getTopicLabel();
        po.status = entity.getStatus() == null ? null : entity.getStatus().name();
        po.definition = entity.getDefinition();
        po.hitStudents = entity.getHitStudents();
        po.hitCount = entity.getHitCount();
        po.promotedBy = entity.getPromotedBy();
        po.createdAt = entity.getCreatedAt();
        po.updatedAt = entity.getUpdatedAt();
        return po;
    }

    public QuestionType toEntity() {
        return QuestionType.restore(
                this.id, this.topicLabel, QuestionTypeStatus.fromCode(this.status), this.definition,
                this.hitStudents, this.hitCount, this.promotedBy, this.createdAt, this.updatedAt);
    }

    public static List<QuestionTypePo> fromList(List<QuestionType> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream().map(QuestionTypePo::from).collect(Collectors.toList());
    }

    public static List<QuestionType> toEntityList(List<QuestionTypePo> pos) {
        if (pos == null) {
            return null;
        }
        return pos.stream().map(QuestionTypePo::toEntity).collect(Collectors.toList());
    }
}
