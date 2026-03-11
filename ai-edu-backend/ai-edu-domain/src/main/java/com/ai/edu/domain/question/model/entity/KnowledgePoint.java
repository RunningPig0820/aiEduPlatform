package com.ai.edu.domain.question.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 知识点实体
 */
@Entity
@Table(name = "t_knowledge_point")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KnowledgePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "subject", length = 50)
    private String subject;

    @Column(name = "grade_level", length = 20)
    private String gradeLevel;

    @Column(columnDefinition = "TEXT")
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