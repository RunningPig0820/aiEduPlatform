package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.GuardResult;
import com.ai.edu.domain.learning.model.contract.ActionMeta;
import com.ai.edu.domain.learning.model.entity.TutoringSession;
import com.ai.edu.domain.learning.model.valueobject.ActionType;
import com.ai.edu.domain.learning.model.valueobject.EndReason;
import com.ai.edu.domain.learning.model.valueobject.TutoringConstants;
import com.ai.edu.domain.learning.model.valueobject.TutoringState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 答疑护栏服务测试（确定性规则，测试重点，逐条覆盖 test.md 护栏单测）。
 */
class TutoringGuardrailServiceTest {

    private final TutoringGuardrailService guardrail = new TutoringGuardrailService();

    // ==================== 答案护栏 ====================

    @Test
    @DisplayName("答案护栏：reveal 且 answer_request_count=0 → DENY + fallback=approach")
    void reveal_unAuthorized_shouldDeny() {
        TutoringSession session = activeSession();

        GuardResult result = guardrail.validate(meta("reveal"), session);

        assertFalse(result.isAllowed());
        assertEquals("answerCountInsufficient", result.getDeniedReason());
        assertEquals(ActionType.APPROACH, result.getFallbackType());
    }

    @Test
    @DisplayName("答案护栏：reveal 且 answer_request_count=1 → ALLOW（第 2 次要答案放行）")
    void reveal_authorized_shouldAllow() {
        TutoringSession session = activeSession();
        session.requestAnswer(); // count 0→1（第 1 次已被拦成思路）

        GuardResult result = guardrail.validate(meta("reveal"), session);

        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("答案护栏：approach 始终 ALLOW（第 1 次出口）")
    void approach_alwaysAllowed() {
        GuardResult result = guardrail.validate(meta("approach"), activeSession());
        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("6.2 重决策仍 reveal → Java 降级固定思路话术 + count→1")
    void degradeRevealToApproach_speechAndCount() {
        TutoringSession session = activeSession();

        String speech = guardrail.degradeRevealToApproach(session);

        assertEquals(TutoringGuardrailService.FALLBACK_APPROACH_SPEECH, speech);
        assertEquals(1, session.getAnswerRequestCount());
        assertTrue(session.isActive());
    }

    // ==================== 轮次护栏 ====================

    @Test
    @DisplayName("轮次护栏：hint 且 round_count=20 → DENY + fallback=end")
    void hint_atRoundLimit_shouldDenyEnd() {
        TutoringSession session = sessionAtRoundLimit();

        GuardResult result = guardrail.validate(meta("hint"), session);

        assertFalse(result.isAllowed());
        assertEquals("roundLimitExceeded", result.getDeniedReason());
        assertEquals(ActionType.END, result.getFallbackType());
    }

    @Test
    @DisplayName("轮次护栏：approach 且 round_count=20 → DENY + fallback=end")
    void approach_atRoundLimit_shouldDenyEnd() {
        TutoringSession session = sessionAtRoundLimit();

        GuardResult result = guardrail.validate(meta("approach"), session);

        assertFalse(result.isAllowed());
        assertEquals(ActionType.END, result.getFallbackType());
    }

    @Test
    @DisplayName("轮次护栏：concept / switch 不消耗轮次，round=20 仍 ALLOW")
    void concept_and_switch_notGuardedByRound() {
        TutoringSession session = sessionAtRoundLimit();

        assertTrue(guardrail.validate(meta("concept"), session).isAllowed());
        assertTrue(guardrail.validate(meta("switch"), session).isAllowed());
        assertTrue(guardrail.validate(meta("end"), session).isAllowed());
    }

    @Test
    @DisplayName("轮次护栏：round<20 正常 hint 放行")
    void hint_belowLimit_shouldAllow() {
        TutoringSession session = activeSession();
        session.recordRound(); // round=1

        assertTrue(guardrail.validate(meta("hint"), session).isAllowed());
    }

    // ==================== 换题 / 收尾 ====================

    @Test
    @DisplayName("换题护栏：type=switch → 计数归零（旧题不点亮），会话保持 ACTIVE")
    void onSwitch_resetsCounters() {
        TutoringSession session = activeSession();
        session.recordRound();
        session.recordRound();
        session.requestAnswer(); // round=2, count=1

        guardrail.onSwitch(session);

        assertEquals(0, session.getRoundCount());
        assertEquals(0, session.getAnswerRequestCount());
        assertEquals(TutoringState.ACTIVE, session.getStatus());
        assertNull(session.getEndReason());
    }

    @Test
    @DisplayName("收尾护栏：type=end → 置 ARCHIVED + endReason")
    void onEnd_archivesWithReason() {
        TutoringSession session = activeSession();

        guardrail.onEnd(session, EndReason.COMPLETED);

        assertEquals(TutoringState.ARCHIVED, session.getStatus());
        assertEquals(EndReason.COMPLETED, session.getEndReason());
        assertNotNull(session.getArchivedAt());
    }

    @Test
    @DisplayName("收尾护栏：end_reason 缺失 → 默认 ABANDONED（不提升掌握度）")
    void onEnd_nullReason_defaultsAbandoned() {
        TutoringSession session = activeSession();

        guardrail.onEnd(session, null);

        assertEquals(EndReason.ABANDONED, session.getEndReason());
    }

    // ==================== 降级与契约（6.3 / 非法 type） ====================

    @Test
    @DisplayName("6.3 结构化输出兜底：degraded=true（type=reveal）→ 按普通 hint 放行，不拦护栏")
    void degraded_true_shouldAllowAsHint() {
        ActionMeta action = ActionMeta.builder().type("reveal").degraded(true).build();

        // 即使 type=reveal，degraded=true 也不走答案护栏，按普通 hint 放行
        assertTrue(guardrail.validate(action, activeSession()).isAllowed());
    }

    @Test
    @DisplayName("非法 type → 走默认 HINT 放行，不阻断")
    void invalidType_defaultsHintAllow() {
        TutoringSession session = activeSession();

        GuardResult result = guardrail.validate(meta("evade"), session);

        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("空 action → 按默认 hint 放行")
    void nullAction_allowed() {
        assertTrue(guardrail.validate(null, activeSession()).isAllowed());
    }

    @Test
    @DisplayName("安全护栏：safety_flag=true → DENY + fallback=end")
    void safetyFlag_denyEnd() {
        ActionMeta action = ActionMeta.builder().type("hint").safetyFlag(true).build();

        GuardResult result = guardrail.validate(action, activeSession());

        assertFalse(result.isAllowed());
        assertEquals("safetyFlagHit", result.getDeniedReason());
        assertEquals(ActionType.END, result.getFallbackType());
    }

    @Test
    @DisplayName("非法 type + round 达上限 → 默认 hint 仍走轮次护栏")
    void invalidType_atRoundLimit_stillGuarded() {
        GuardResult result = guardrail.validate(meta("garbage"), sessionAtRoundLimit());
        assertFalse(result.isAllowed());
        assertEquals(ActionType.END, result.getFallbackType());
    }

    // ==================== helpers ====================

    private TutoringSession activeSession() {
        return TutoringSession.start(1001L, "math");
    }

    private TutoringSession sessionAtRoundLimit() {
        TutoringSession session = activeSession();
        for (int i = 0; i < TutoringConstants.SESSION_ROUND_LIMIT; i++) {
            session.recordRound(); // round 0→20
        }
        assertEquals(TutoringConstants.SESSION_ROUND_LIMIT, session.getRoundCount());
        return session;
    }

    private ActionMeta meta(String type) {
        return ActionMeta.builder().type(type).build();
    }
}
