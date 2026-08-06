package com.ai.edu.domain.learning.model.entity;

/**
 * 答疑轮次上限领域异常——{@link TutoringSession#recordRound()} 在轮次已满时抛出。
 *
 * <p>正常路径下由护栏（6.1）在调用前拦截并转 end(ROUND_LIMIT) 收尾；
 * 本异常是聚合不变量的防御性兜底（round_count ≤ SESSION_ROUND_LIMIT）。
 */
public class TutoringRoundLimitException extends IllegalStateException {

    public TutoringRoundLimitException(int limit) {
        super("答疑轮次已达上限: " + limit);
    }
}
