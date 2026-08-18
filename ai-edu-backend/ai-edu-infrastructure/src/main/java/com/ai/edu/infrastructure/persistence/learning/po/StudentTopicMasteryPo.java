package com.ai.edu.infrastructure.persistence.learning.po;

import com.ai.edu.domain.learning.model.entity.StudentTopicMastery;
import com.ai.edu.domain.learning.model.valueobject.MasteryLevel;
import com.ai.edu.domain.learning.model.valueobject.TopicKey;
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
 * 学生题型掌握度持久化对象（表：t_student_topic_mastery，ai_edu_learning 库）。
 *
 * <p>以归一化题型名（topic_key）为 key（student_id + topic_key 唯一），UPSERT 幂等。
 * mastery_level 为连续百分比（累计平均正确率）；train_count 训练数；source 来源（ai/bank）。
 * evidence 为 JSON 字符串列（命中步骤、错误事件 id 列表）。
 */
@TableName("t_student_topic_mastery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentTopicMasteryPo {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private Long studentId;

    @TableField("topic_key")
    private String topicKey;

    @TableField("topic_label")
    private String topicLabel;

    @TableField("mastery_level")
    private Integer masteryLevel;

    @TableField("evidence")
    private String evidence;

    @TableField("last_session_id")
    private Long lastSessionId;

    @TableField("source")
    private String source;

    @TableField("train_count")
    private Long trainCount;

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

    public static StudentTopicMasteryPo from(StudentTopicMastery entity) {
        if (entity == null) {
            return null;
        }
        StudentTopicMasteryPo po = new StudentTopicMasteryPo();
        po.id = entity.getId();
        po.studentId = entity.getStudentId();
        po.topicKey = entity.getTopicKey() == null ? null : entity.getTopicKey().getValue();
        po.topicLabel = entity.getTopicLabel();
        po.masteryLevel = entity.getMasteryLevel() == null ? 0 : entity.getMasteryLevel().getValue();
        po.evidence = entity.getEvidence();
        po.lastSessionId = entity.getLastSessionId();
        po.source = entity.getSource();
        po.trainCount = entity.getTrainCount();
        po.updatedAt = entity.getUpdatedAt();
        return po;
    }

    public StudentTopicMastery toEntity() {
        TopicKey key = this.topicKey == null || this.topicKey.isBlank() ? null : TopicKey.of(this.topicKey);
        MasteryLevel level = this.masteryLevel == null ? MasteryLevel.notStarted() : MasteryLevel.of(this.masteryLevel);
        return StudentTopicMastery.restore(
                this.id, this.studentId, key, this.topicLabel, level,
                this.evidence, this.lastSessionId, this.source,
                this.trainCount == null ? 0L : this.trainCount, this.updatedAt);
    }

    public static List<StudentTopicMasteryPo> fromList(List<StudentTopicMastery> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream().map(StudentTopicMasteryPo::from).collect(Collectors.toList());
    }

    public static List<StudentTopicMastery> toEntityList(List<StudentTopicMasteryPo> pos) {
        if (pos == null) {
            return null;
        }
        return pos.stream().map(StudentTopicMasteryPo::toEntity).collect(Collectors.toList());
    }
}
