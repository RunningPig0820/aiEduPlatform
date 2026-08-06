package com.ai.edu.domain.learning.model.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 学生回答评估（eval 软信号，Java 放宽处理，Java↔Python 内部契约 snake_case）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 回答是否正确 */
    private Boolean correct;

    /** 错误类型（可空） */
    @JsonProperty("error_type")
    private String errorType;

    /** 情绪（F7 七态字符串：NEUTRAL/CONFUSED/...，Python 输出方权威） */
    private String emotion;

    /** 是否独立解出 */
    @JsonProperty("exercise_complete")
    private Boolean exerciseComplete;
}
