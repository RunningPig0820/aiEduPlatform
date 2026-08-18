package com.ai.edu.application.dto.learning;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生题目记录单项（tasks 4.2：按题型查题目，掌握度页「查看题目」）。
 *
 * <p>掌握度页点「查看题目」→ 该题型全部证据题（content/score/session_id 原题链接）。
 * sessionId 可跳回答疑会话看原题；无会话链接为 null（显示题目原文）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentQuestionItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 题目记录 ID */
    private Long id;

    /** 题目文本（该轮题目） */
    private String content;

    /** 来源：ai / bank */
    private String source;

    /** 生效分值 0.00/0.50/1.00（含打折，与掌握表聚合同源可追溯） */
    private BigDecimal score;

    /** 引导轮数（roundCount） */
    private Integer hintCount;

    /** 要答案次数（answerRequestCount） */
    private Integer answerRequestCount;

    /** 原题链接：答疑会话 ID（可跳回看原题，无则显示题目原文） */
    private Long sessionId;

    /** 作答时间 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
