package com.ai.edu.domain.learning.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 一轮作答信号（题目聚合的原子单位，tasks 3.1.2）。
 *
 * <p>decide 每轮输出一轮作答结果（eval.correct + 是否有引导），聚合进 {@link QuestionAttempt}；
 * 3.3 信号映射消费轮信号算该题生效分值（直接答对 1.0 / 引导后答对 0.5 / 答错 0.0）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundSignal implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 该轮评估是否正确（eval.correct） */
    private boolean correct;

    /** 该轮是否有引导（hint/approach 或要答案）——3.3 区分「直接答对」与「引导后答对」 */
    private boolean hinted;

    /** 该轮序号（1 起） */
    private int roundNumber;
}
