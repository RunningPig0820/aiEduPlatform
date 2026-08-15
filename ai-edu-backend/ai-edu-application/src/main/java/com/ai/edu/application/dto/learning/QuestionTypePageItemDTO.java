package com.ai.edu.application.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 题型库分页条目（GET /api/kp/question-types）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionTypePageItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 题型 ID */
    private Long id;

    /** 题型名 */
    private String topicLabel;

    /** CANDIDATE / STABLE */
    private String status;

    /** 总命中次数 */
    private Integer hitCount;
}
