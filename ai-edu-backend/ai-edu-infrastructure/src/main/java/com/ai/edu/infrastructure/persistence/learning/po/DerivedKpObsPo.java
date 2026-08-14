package com.ai.edu.infrastructure.persistence.learning.po;

import com.ai.edu.domain.learning.model.entity.DerivedKpObs;
import com.ai.edu.domain.learning.model.valueobject.DerivedKpSource;
import com.ai.edu.domain.learning.model.valueobject.DerivedKpStatus;
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
 * 个体派生观测持久化对象（表：t_kp_derived_obs，ai_edu_learning 库）。
 *
 * <p>source/status 存枚举 name 字符串；kp_uri 可空（PENDING）。
 */
@TableName("t_kp_derived_obs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DerivedKpObsPo {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private Long studentId;

    @TableField("topic_label")
    private String topicLabel;

    @TableField("kp_uri")
    private String kpUri;

    @TableField("student_grade")
    private Integer studentGrade;

    @TableField("confidence")
    private Integer confidence;

    @TableField("source")
    private String source;

    @TableField("status")
    private String status;

    @TableField("occurrence_count")
    private Integer occurrenceCount;

    @TableField("first_seen_at")
    private LocalDateTime firstSeenAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("created_by")
    private Long createdBy = 0L;

    @TableField("modified_by")
    private Long modifiedBy = 0L;

    @TableField("is_deleted")
    private Boolean deleted = false;

    public static DerivedKpObsPo from(DerivedKpObs entity) {
        if (entity == null) {
            return null;
        }
        DerivedKpObsPo po = new DerivedKpObsPo();
        po.id = entity.getId();
        po.studentId = entity.getStudentId();
        po.topicLabel = entity.getTopicLabel();
        po.kpUri = entity.getKpUri();
        po.studentGrade = entity.getStudentGrade();
        po.confidence = entity.getConfidence();
        po.source = entity.getSource() == null ? null : entity.getSource().name();
        po.status = entity.getStatus() == null ? null : entity.getStatus().name();
        po.occurrenceCount = entity.getOccurrenceCount();
        po.firstSeenAt = entity.getFirstSeenAt();
        po.updatedAt = entity.getUpdatedAt();
        return po;
    }

    public DerivedKpObs toEntity() {
        return DerivedKpObs.restore(
                this.id, this.studentId, this.topicLabel, this.kpUri, this.studentGrade,
                this.confidence, DerivedKpSource.fromCode(this.source),
                DerivedKpStatus.fromCode(this.status), this.occurrenceCount,
                this.firstSeenAt, this.updatedAt);
    }

    public static List<DerivedKpObsPo> fromList(List<DerivedKpObs> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream().map(DerivedKpObsPo::from).collect(Collectors.toList());
    }

    public static List<DerivedKpObs> toEntityList(List<DerivedKpObsPo> pos) {
        if (pos == null) {
            return null;
        }
        return pos.stream().map(DerivedKpObsPo::toEntity).collect(Collectors.toList());
    }
}
