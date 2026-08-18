package com.ai.edu.application.dto.learning;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学生题型掌握度单项（掌握度主体翻转：题型粒度，tasks 4.1）。
 *
 * <p>masteryLevel = 0-100 连续百分比（累计平均正确率），source = ai/bank，trainCount = 训练数。
 * status=RESOLVED（掌握表已锚定）/ PENDING（题目记录有但 canonical 未归属，域 B 独立化 Decision 10）。
 * 前端掌握度页列式展示：题型 | 来源 | 掌握% | 训练数 | [查看题目]。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasteryItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 归一化题型标识（幂等主键） */
    private String topicKey;

    /** 题型展示名 */
    private String topicLabel;

    /** 掌握度 0-100 连续百分比（累计平均正确率，BREAKING：原四档 0/25/50/75） */
    private Integer masteryLevel;

    /** 来源：ai（AI 答疑）/ bank（题库） */
    private String source;

    /** 训练数（该题型已练题目数，累计平均分母） */
    private Integer trainCount;

    /** 解析状态：RESOLVED（已锚定）/ PENDING（题目有但 canonical 未归属） */
    private String status;

    /** 解析置信度 0-100（从派生观测关联） */
    private Integer confidence;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
