package com.ai.edu.infrastructure.persistence.learning.po;

import com.ai.edu.domain.learning.model.entity.StudentKpMastery;
import com.ai.edu.domain.learning.model.valueobject.KpKey;
import com.ai.edu.domain.learning.model.valueobject.MasteryLevel;
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
 * 学生知识点掌握度持久化对象（表：t_student_kp_mastery，ai_edu_learning 库）。
 *
 * <p>以 TextbookKP URI 为 key（student_id + kp_key 唯一），UPSERT 幂等。
 * evidence 为 JSON 字符串列（命中步骤、错误事件 id 列表）。
 */
@TableName("t_student_kp_mastery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentKpMasteryPo {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private Long studentId;

    @TableField("kp_key")
    private String kpKey;

    @TableField("kp_label")
    private String kpLabel;

    @TableField("mastery_level")
    private Integer masteryLevel;

    @TableField("evidence")
    private String evidence;

    @TableField("last_session_id")
    private Long lastSessionId;

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

    public static StudentKpMasteryPo from(StudentKpMastery entity) {
        if (entity == null) {
            return null;
        }
        StudentKpMasteryPo po = new StudentKpMasteryPo();
        po.id = entity.getId();
        po.studentId = entity.getStudentId();
        po.kpKey = entity.getKpKey() == null ? null : entity.getKpKey().getValue();
        po.kpLabel = entity.getKpLabel();
        po.masteryLevel = entity.getMasteryLevel() == null ? 0 : entity.getMasteryLevel().getValue();
        po.evidence = entity.getEvidence();
        po.lastSessionId = entity.getLastSessionId();
        po.updatedAt = entity.getUpdatedAt();
        return po;
    }

    public StudentKpMastery toEntity() {
        KpKey key = this.kpKey == null || this.kpKey.isBlank() ? null : KpKey.of(this.kpKey);
        MasteryLevel level = this.masteryLevel == null ? MasteryLevel.notStarted() : MasteryLevel.of(this.masteryLevel);
        return StudentKpMastery.restore(
                this.id, this.studentId, key, this.kpLabel, level,
                this.evidence, this.lastSessionId, this.updatedAt);
    }

    public static List<StudentKpMasteryPo> fromList(List<StudentKpMastery> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream().map(StudentKpMasteryPo::from).collect(Collectors.toList());
    }

    public static List<StudentKpMastery> toEntityList(List<StudentKpMasteryPo> pos) {
        if (pos == null) {
            return null;
        }
        return pos.stream().map(StudentKpMasteryPo::toEntity).collect(Collectors.toList());
    }
}
