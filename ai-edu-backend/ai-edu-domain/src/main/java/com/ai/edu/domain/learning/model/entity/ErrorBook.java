package com.ai.edu.domain.learning.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 错题实体
 */
@TableName("t_error_book")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorBook {

    @com.baomidou.mybatisplus.annotation.TableId(type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private Long studentId;

    @TableField("question_id")
    private Long questionId;

    @TableField("homework_id")
    private Long homeworkId;

    @TableField("error_count")
    private Integer errorCount = 1;

    @TableField("is_corrected")
    private Boolean isCorrected = false;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("last_error_at")
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