package com.ai.edu.domain.homework.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 作业实体
 */
@Entity
@Table(name = "t_homework")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Homework {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "score")
    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    public static Homework create(Long studentId, Long questionId, String imageUrl) {
        Homework homework = new Homework();
        homework.studentId = studentId;
        homework.questionId = questionId;
        homework.imageUrl = imageUrl;
        homework.status = "PENDING";
        return homework;
    }

    public void markAsProcessing() {
        this.status = "PROCESSING";
    }

    public void complete(Integer score, String feedback) {
        this.status = "COMPLETED";
        this.score = score;
        this.feedback = feedback;
    }

    public void fail(String feedback) {
        this.status = "FAILED";
        this.feedback = feedback;
    }
}