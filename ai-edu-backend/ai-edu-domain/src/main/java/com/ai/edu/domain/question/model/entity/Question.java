package com.ai.edu.domain.question.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 题目实体
 */
@TableName(value = "t_question", autoResultMap = true)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    @com.baomidou.mybatisplus.annotation.TableId(type = IdType.AUTO)
    private Long id;

    @TableField("content")
    private String content;

    @TableField("question_type")
    private String questionType;

    @TableField("difficulty")
    private String difficulty;

    @TableField("knowledge_point_id")
    private Long knowledgePointId;

    @TableField("knowledge_point_name")
    private String knowledgePointName;

    @TableField("answer")
    private String answer;

    @TableField("analysis")
    private String analysis;

    @TableField("created_by")
    private Long createdBy;

    @TableField(value = "options", typeHandler = JacksonTypeHandler.class)
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