package com.ai.edu.domain.edukg.model.entity;

import com.ai.edu.domain.edukg.model.valueobject.KgDifficulty;
import com.ai.edu.domain.edukg.model.valueobject.KgImportance;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 知识图谱-知识点实体
 */
@TableName("t_kg_knowledge_point")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgKnowledgePoint {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("uri")
    private String uri;

    @TableField("label")
    private String label;

    @TableField("difficulty")
    private String difficulty;

    @TableField("importance")
    private String importance;

    @TableField("cognitive_level")
    private String cognitiveLevel;

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

    public static KgKnowledgePoint create(String uri, String label) {
        KgKnowledgePoint kp = new KgKnowledgePoint();
        kp.uri = uri;
        kp.label = label;
        kp.status = "active";
        return kp;
    }

    public void updateAttributes(String difficulty, String importance, String cognitiveLevel) {
        this.difficulty = difficulty;
        this.importance = importance;
        this.cognitiveLevel = cognitiveLevel;
    }

    public boolean isHighImport() {
        return KgImportance.HIGH.getValue().equals(this.importance);
    }
}
