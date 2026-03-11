package com.ai.edu.domain.learning.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 错题实体
 */
@Entity
@Table(name = "t_error_book")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "homework_id")
    private Long homeworkId;

    @Column(name = "error_count", nullable = false)
    private Integer errorCount = 1;

    @Column(name = "is_corrected", nullable = false)
    private Boolean isCorrected = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_error_at")
    private LocalDateTime lastErrorAt;

    public static ErrorBook create(Long studentId, Long questionId) {
        ErrorBook errorBook = new ErrorBook();
        errorBook.studentId = studentId;
        errorBook.questionId = questionId;
        errorBook.createdAt = LocalDateTime.now();
        errorBook.lastErrorAt = LocalDateTime.now();
        return errorBook;
    }

    public void incrementErrorCount() {
        this.errorCount++;
        this.lastErrorAt = LocalDateTime.now();
    }

    public void markAsCorrected() {
        this.isCorrected = true;
    }

    public void resetCorrection() {
        this.isCorrected = false;
    }
}