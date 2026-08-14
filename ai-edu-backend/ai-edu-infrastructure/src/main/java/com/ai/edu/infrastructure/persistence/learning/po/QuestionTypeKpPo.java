package com.ai.edu.infrastructure.persistence.learning.po;

import com.ai.edu.domain.learning.model.entity.QuestionTypeKp;
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
 * 题型↔知识点 年级分布桶持久化对象（表：t_kp_question_type_kp，ai_edu_learning 库）。
 */
@TableName("t_kp_question_type_kp")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionTypeKpPo {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("question_type_id")
    private Long questionTypeId;

    @TableField("kp_uri")
    private String kpUri;

    @TableField("grade_range")
    private String gradeRange;

    @TableField("hit_students")
    private Integer hitStudents;

    @TableField("hit_count")
    private Integer hitCount;

    @TableField("ratio")
    private Double ratio;

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

    public static QuestionTypeKpPo from(QuestionTypeKp entity) {
        if (entity == null) {
            return null;
        }
        QuestionTypeKpPo po = new QuestionTypeKpPo();
        po.id = entity.getId();
        po.questionTypeId = entity.getQuestionTypeId();
        po.kpUri = entity.getKpUri();
        po.gradeRange = entity.getGradeRange();
        po.hitStudents = entity.getHitStudents();
        po.hitCount = entity.getHitCount();
        po.ratio = entity.getRatio();
        po.createdAt = entity.getCreatedAt();
        po.updatedAt = entity.getUpdatedAt();
        return po;
    }

    public QuestionTypeKp toEntity() {
        return QuestionTypeKp.restore(
                this.id, this.questionTypeId, this.kpUri, this.gradeRange,
                this.hitStudents, this.hitCount, this.ratio, this.createdAt, this.updatedAt);
    }

    public static List<QuestionTypeKpPo> fromList(List<QuestionTypeKp> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream().map(QuestionTypeKpPo::from).collect(Collectors.toList());
    }

    public static List<QuestionTypeKp> toEntityList(List<QuestionTypeKpPo> pos) {
        if (pos == null) {
            return null;
        }
        return pos.stream().map(QuestionTypeKpPo::toEntity).collect(Collectors.toList());
    }
}
