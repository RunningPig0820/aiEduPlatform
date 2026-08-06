package com.ai.edu.application.service.learning;

import com.ai.edu.domain.learning.model.contract.ActionMeta;
import com.ai.edu.application.dto.learning.GuardResult;
import com.ai.edu.domain.learning.model.entity.TutoringSession;
import com.ai.edu.domain.learning.model.valueobject.ActionType;
import com.ai.edu.domain.learning.model.valueobject.EndReason;
import com.ai.edu.domain.learning.model.valueobject.TutoringConstants;
import com.ai.edu.domain.learning.service.TutoringConfig;
import jakarta.annotation.Resource;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 答疑护栏服务——Java 动作出口的确定性规则引擎（设计决策 4，测试重点）。
 *
 * <p>Python decide 输出的 action 在此校验放行/拒绝，保证任何内容流入学生之前 type 已过护栏：
 * <ul>
 *   <li><b>答案护栏</b>：{@code type=reveal} 且 {@code answer_request_count < 1}（未授权）→ DENY + fallback=approach（count→1 由编排服务调用 {@link TutoringSession#requestAnswer()} 执行）</li>
 *   <li><b>轮次护栏</b>：引导类（hint/approach）且 {@code round_count >= 20} → DENY + fallback=end(ROUND_LIMIT)</li>
 *   <li><b>安全护栏</b>：decide 输出 {@code safety_flag=true} → DENY + fallback=end（终止会话，无 token 流）</li>
 *   <li><b>换题/收尾</b>：{@code switch} / {@code end} 放行，会话侧副作用见 {@link #onSwitch}/{@link #onEnd}</li>
 * </ul>
 *
 * <p>非法/缺失 type 走默认 HINT 放行（设计：不阻断）；Python 结构化输出兜底
 * （200 + ActionMeta(type=hint, degraded=true)）按普通 hint 放行 + 记日志（{@link #validate}），不使用 503。
 *
 * <p>本服务只做确定性判定 + 会话对象上的确定性副作用，不触 I/O；
 * 掌握度校正（COMPLETED 提升 75+，需 KpResolver + 掌握度仓储）由编排服务在 {@link #onEnd} 后执行。
 */
@Slf4j
@Service
public class TutoringGuardrailService {

    /** 6.2 reveal 被拒后重决策仍 reveal 时 Java 直接降级的固定思路话术（不依赖 LLM）。 */
    public static final String FALLBACK_APPROACH_SPEECH =
            "先设出未知数，根据题意找出等量关系列方程（组），再联立求解，最后代回检验是否符合题意。";

    @Resource
    @Setter(AccessLevel.PACKAGE)
    private TutoringConfig tutoringConfig;

    /** 轮次上限（配置值；未注入配置时回退领域常量，保持测试/默认行为一致）。 */
    private int roundLimit() {
        return tutoringConfig == null ? TutoringConstants.SESSION_ROUND_LIMIT : tutoringConfig.roundLimit();
    }

    /**
     * 校验动作（纯规则，无 I/O）。
     *
     * @param action  Python decide 输出（可含非法/缺失字段，此处容错）
     * @param session 当前会话（含护栏计数器）
     * @return GuardResult.ALLOW 放行；DENY 拒绝并带 fallbackType（编排服务据此降级）
     */
    public GuardResult validate(ActionMeta action, TutoringSession session) {
        if (action == null) {
            log.warn("[tutoring] decide 返回空 action，按默认 hint 放行, sessionId={}", session.getId());
            return GuardResult.allow();
        }

        // 6.3 结构化输出兜底：type=hint + degraded=true → 按普通 hint 放行 + 记日志（监控用），不拦护栏，不使用 503
        if (Boolean.TRUE.equals(action.getDegraded())) {
            log.warn("[tutoring] decide degraded=true，按普通 hint 放行, sessionId={}", session.getId());
            return GuardResult.allow();
        }

        // 安全护栏：decide 输出 safety_flag → 拒绝（终止会话由编排服务执行，无 token 流）
        if (Boolean.TRUE.equals(action.getSafetyFlag())) {
            log.warn("[tutoring] decide safety_flag=true，终止会话, sessionId={}", session.getId());
            return GuardResult.deny("safetyFlagHit", ActionType.END);
        }

        // 非法/缺失 type 走默认 HINT（设计：不阻断），再走轮次护栏
        ActionType type = ActionType.fromCodeOrDefault(action.getType());

        // 答案护栏：reveal 且未授权（answer_request_count < 1）→ 拒绝降级为 approach
        if (type == ActionType.REVEAL && session.getAnswerRequestCount() < 1) {
            log.info("[tutoring] 答案护栏拦截 reveal（未授权, count={}）, sessionId={}",
                    session.getAnswerRequestCount(), session.getId());
            return GuardResult.deny("answerCountInsufficient", ActionType.APPROACH);
        }

        // 轮次护栏：引导类（hint/approach）且 round_count >= roundLimit → 强制收尾 end(ROUND_LIMIT)
        int roundLimit = roundLimit();
        if ((type == ActionType.HINT || type == ActionType.APPROACH)
                && session.getRoundCount() >= roundLimit) {
            log.info("[tutoring] 轮次护栏：达 {} 轮上限，强制收尾 ROUND_LIMIT, sessionId={}",
                    roundLimit, session.getId());
            return GuardResult.deny("roundLimitExceeded", ActionType.END);
        }

        // concept / switch / end / reveal(已授权) / hint / approach → 放行
        return GuardResult.allow();
    }

    /**
     * 换题护栏（type=switch 放行后）：仅重置计数（round/answer 归零按新题重计）。
     *
     * <p>旧题知识点不校正（不点亮）——Java 不记录题目内容，换题判定全在 Python decide；
     * 会话保持 ACTIVE（新题继续），无独立归档记录。
     */
    public void onSwitch(TutoringSession session) {
        session.switchQuestion();
        log.info("[tutoring] 换题：计数重置, sessionId={}", session.getId());
    }

    /**
     * 收尾护栏（type=end 放行后）：置 ARCHIVED + endReason。
     *
     * <p>掌握度校正（COMPLETED 提升 75+ / 其余不提升）由编排服务执行
     * （需 KpResolver 解析 label→URI + StudentKpMasteryRepository）。
     */
    public void onEnd(TutoringSession session, EndReason reason) {
        EndReason actual = (reason == null) ? EndReason.ABANDONED : reason;
        session.complete(actual);
        log.info("[tutoring] 收尾归档, sessionId={}, endReason={}", session.getId(), actual);
    }

    /**
     * 6.2 reveal 被拒后重决策仍 reveal → Java 直接降级固定思路话术 + count→1。
     *
     * <p>视为第 1 次要答案（answer_request_count→1），下次再 reveal 即放行；
     * 返回固定思路话术（不依赖 LLM，保证兜底可用）。
     *
     * @return 固定思路话术
     */
    public String degradeRevealToApproach(TutoringSession session) {
        session.requestAnswer();
        log.info("[tutoring] reveal 重决策仍 reveal，Java 降级固定思路话术, sessionId={}", session.getId());
        return FALLBACK_APPROACH_SPEECH;
    }
}
