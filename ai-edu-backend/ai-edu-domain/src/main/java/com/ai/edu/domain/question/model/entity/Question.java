package com.ai.edu.domain.question.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * 题目实体
 */
@Entity
@Table(name = "t_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "question_type", nullable = false, length = 20)
    private String questionType;

    @Column(nullable = false, length = 20)
    private String difficulty;

    @Column(name = "knowledge_point_id")
    private Long knowledgePointId;

    @Column(name = "knowledge_point_name", length = 100)
    private String knowledgePointName;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(columnDefinition = "TEXT")
    private String analysis;

    @Column(name = "created_by")
    private Long createdBy;

    @ElementCollection
    @CollectionTable(name = "t_question_options", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_content")
    private List<String> options = new ArrayList<>();

    public static Question create(String content, String questionType, String difficulty) {
        Question question = new Question();
        question.content = content;
        question.questionType = questionType;
        question.difficulty = difficulty;
        return question;
    }

    public void setKnowledgePoint(Long pointId, String pointName) {
        this.knowledgePointId = pointId;
        this.knowledgePointName = pointName;
    }

    public void setAnswer(String answer, String analysis) {
        this.answer = answer;
        this.analysis = analysis;
    }

    public void addOption(String option) {
        this.options.add(option);
    }
}