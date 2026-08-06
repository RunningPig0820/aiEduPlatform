package com.ai.edu.application.dto.learning;

import com.ai.edu.domain.learning.model.valueobject.ActionType;
import lombok.Getter;

/**
 * 护栏校验结果（内部对象，非 JSON DTO）。
 *
 * <p>ALLOW：放行动作；DENY：拒绝并给出降级 fallbackType（如 reveal 未授权 → approach）。
 */
@Getter
public class GuardResult {

    public enum Decision {
        ALLOW, DENY
    }

    private final Decision decision;
    private final String deniedReason;
    private final ActionType fallbackType;

    private GuardResult(Decision decision, String deniedReason, ActionType fallbackType) {
        this.decision = decision;
        this.deniedReason = deniedReason;
        this.fallbackType = fallbackType;
    }

    public static GuardResult allow() {
        return new GuardResult(Decision.ALLOW, null, null);
    }

    public static GuardResult deny(String deniedReason, ActionType fallbackType) {
        return new GuardResult(Decision.DENY, deniedReason, fallbackType);
    }

    public boolean isAllowed() {
        return decision == Decision.ALLOW;
    }
}
