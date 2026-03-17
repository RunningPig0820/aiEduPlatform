package com.ai.edu.domain.question.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 知识点实体
 */
@TableName("t_knowledge_point")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KnowledgePoint {

    @com.baomidou.mybatisplus.annotation.TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("parent_id")
    private Long parentId;

    @TableField("subject")
    private String subject;

    @TableField("grade_level")
    private String gradeLevel;

    @TableField("description")
    private String description;

    public static KnowledgePoint create(String name, String subject) {
        KnowledgePoint point = new KnowledgePoint();
        point.name = name;
        point.subject = subject;
        return point;
    }

    public void setParent(Long parentId) {
        this.parentId = parentId;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}