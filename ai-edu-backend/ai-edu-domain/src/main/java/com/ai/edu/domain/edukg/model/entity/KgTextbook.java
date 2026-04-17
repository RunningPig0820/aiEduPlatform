package com.ai.edu.domain.edukg.model.entity;

import com.ai.edu.domain.edukg.model.valueobject.KgNodeStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 知识图谱-教材实体
 */
@TableName("t_kg_textbook")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgTextbook {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("uri")
    private String uri;

    @TableField("label")
    private String label;

    @TableField("grade")
    private String grade;

    @TableField("phase")
    private String phase;

    @TableField("subject")
    private String subject;

    @TableField("status")
    private String status;

    @TableField("merged_to_uri")
    private String mergedToUri;

    @TableField("created_by")
    private Long createdBy = 0L;

    @TableField("modified_by")
    private Long modifiedBy = 0L;

    @TableField("is_deleted")
    private Boolean deleted = false;

    public static KgTextbook create(String uri, String label, String grade, String phase, String subject) {
        KgTextbook textbook = new KgTextbook();
        textbook.uri = uri;
        textbook.label = label;
        textbook.grade = grade;
        textbook.phase = phase;
        textbook.subject = subject;
        textbook.status = "active";
        return textbook;
    }

    public void markDeleted(String mergedToUri) {
        this.status = KgNodeStatus.MERGED.getValue();
        this.mergedToUri = mergedToUri;
    }

    public void updateFrom(KgTextbook other) {
        this.label = other.label;
        this.grade = other.grade;
        this.phase = other.phase;
        this.subject = other.subject;
    }

    public boolean isMerged() {
        return KgNodeStatus.MERGED.getValue().equals(this.status);
    }
}
