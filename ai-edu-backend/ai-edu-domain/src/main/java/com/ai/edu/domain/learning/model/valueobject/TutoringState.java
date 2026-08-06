package com.ai.edu.domain.learning.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;

/**
 * 答疑会话生命周期状态（固定 3 态）。
 *
 * <p>无流程状态机——答疑对话流程由 agent 上下文承载，Java 只保留生命周期 3 态
 * + 护栏计数器（round_count / answer_request_count），不随题目/对话/换题增长。
 */
public enum TutoringState implements ValueObject {
    /** 进行中 */
    ACTIVE,
    /** 已归档（正常收尾） */
    ARCHIVED,
    /** 已终止（无关/非数学/安全等终止场景） */
    TERMINATED;

    /**
     * 容错解析（数据库/响应值）。未知或 null 返回 null，由调用方决定默认。
     */
    public static TutoringState fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return TutoringState.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
