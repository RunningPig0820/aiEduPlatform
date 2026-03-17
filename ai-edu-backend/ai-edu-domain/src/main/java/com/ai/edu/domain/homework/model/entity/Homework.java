package com.ai.edu.domain.homework.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 作业实体
 */
@TableName("t_homework")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Homework {

    @com.baomidou.mybatisplus.annotation.TableId(type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private Long studentId;

    @TableField("question_id")
    private Long questionId;

    @TableField("image_url")
    private String imageUrl;

    @TableField("status")
    private String status = "PENDING";

    @TableField("score")
    private Integer score;

    @TableField("feedback")
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