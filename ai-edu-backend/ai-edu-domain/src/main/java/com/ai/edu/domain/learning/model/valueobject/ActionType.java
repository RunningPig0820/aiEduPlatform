package com.ai.edu.domain.learning.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;

/**
 * 答疑动作类型（闭集）——Python decide 输出的硬信号，Java 护栏据此放行/拒绝。
 *
 * <p>decide 输出非法 type 时走默认 HINT（设计：非法 type → 默认 hint，不阻断；
 * Python 结构化输出兜底 ActionMeta(type=hint, degraded=true) 也由本逻辑覆盖）。
 */
public enum ActionType implements ValueObject {
    /** 引导提示（一条反问，不含步骤、不含数值） */
    HINT,
    /** 思路大纲（步骤+关键公式，不含完整演算与最终数值） */
    APPROACH,
    /** 完整解答（需答案护栏放行） */
    REVEAL,
    /** 澄清/追问（过简/模糊输入，不终止会话） */
    CONCEPT,
    /** 换题（new_question 必填） */
    SWITCH,
    /** 收尾（end_reason 联动） */
    END;

    /** 容错解析：未知或 null 返回 null。 */
    public static ActionType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return ActionType.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 容错解析并给默认值：未知或 null → HINT。 */
    public static ActionType fromCodeOrDefault(String code) {
        ActionType actionType = fromCode(code);
        return actionType != null ? actionType : HINT;
    }
}
