package com.ai.edu.domain.learning.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 题目聚合 {@link QuestionAttempt} 值对象测试（tasks 3.1：一道题多轮信号合并成一条）。
 *
 * <p>题聚合是「一道题的一次完整作答」的跨轮状态容器：累计该题各轮信号（{@link RoundSignal}），
 * 换题/结束经 {@link TutoringSession#settleAttempt()} 结算——3.4 落题目表时消费聚合信号。
 */
class QuestionAttemptTest {

    @Test
    @DisplayName("beginQuestion 后初始空聚合（无轮信号，roundCount=0）")
    void beginQuestion_startsEmpty() {
        TutoringSession session = TutoringSession.start(1001L, "math");
        session.beginQuestion();

        QuestionAttempt attempt = session.getCurrentAttempt();
        assertEquals(0, attempt.getRoundCount());
        assertTrue(attempt.getRounds().isEmpty(), "新题无轮信号");
    }

    @Test
    @DisplayName("onRoundSignal 多轮累计：3 轮（直接答对/引导后答对/答错）→ rounds=3，信号逐轮保留")
    void onRoundSignal_accumulatesRounds() {
        TutoringSession session = TutoringSession.start(1001L, "math");
        session.beginQuestion();
        session.onRoundSignal(true, false);   // 直接答对
        session.onRoundSignal(true, true);    // 引导后答对
        session.onRoundSignal(false, false);  // 答错

        QuestionAttempt attempt = session.getCurrentAttempt();
        assertEquals(3, attempt.getRoundCount());
        assertEquals(3, attempt.getRounds().size());
        assertTrue(attempt.getRounds().get(0).isCorrect());
        assertFalse(attempt.getRounds().get(0).isHinted());
        assertTrue(attempt.getRounds().get(1).isHinted(), "引导后答对 hinted=true");
        assertFalse(attempt.getRounds().get(2).isCorrect());
        assertEquals(1, attempt.getRounds().get(0).getRoundNumber());
        assertEquals(3, attempt.getRounds().get(2).getRoundNumber());
    }

    @Test
    @DisplayName("settleAttempt 换题结算：返回当前题聚合并重置新题（一道题一条聚合）")
    void settleAttempt_settlesAndResets() {
        TutoringSession session = TutoringSession.start(1001L, "math");
        session.beginQuestion();
        session.recordAttemptTopic("鸡兔同笼");
        session.onRoundSignal(true, false);
        session.onRoundSignal(false, true);

        QuestionAttempt settled = session.settleAttempt(); // SWITCH 换题触发

        assertEquals(2, settled.getRounds().size());
        assertEquals("鸡兔同笼", settled.getTopicLabel());
        // 已重置为新题（下次作答从零累计）
        assertEquals(0, session.getCurrentAttempt().getRoundCount());
        assertTrue(session.getCurrentAttempt().getRounds().isEmpty());
    }

    @Test
    @DisplayName("recordAttemptTopic 记录当前题题型名（3.4 过聚集用）")
    void recordAttemptTopic_setsTopic() {
        TutoringSession session = TutoringSession.start(1001L, "math");
        session.beginQuestion();
        session.recordAttemptTopic("一元二次方程");
        assertEquals("一元二次方程", session.getCurrentAttempt().getTopicLabel());
    }

    @Test
    @DisplayName("3.2 题目文本: 新题开始 pending 等待首条 user 消息 → recordQuestionContent 捕获 + pending 复位")
    void recordQuestionContent_capturesFirstUserMessage() {
        TutoringSession session = TutoringSession.start(1001L, "math");
        session.beginQuestion();
        assertTrue(session.isContentCapturePending(), "新题开始等待捕获题目文本");

        session.recordQuestionContent("笼子里有鸡和兔共 35 个头 94 只脚");

        assertEquals("笼子里有鸡和兔共 35 个头 94 只脚", session.getCurrentAttempt().getContent());
        assertFalse(session.isContentCapturePending(), "捕获后 pending 复位，后续「提示一下」不更新");
    }
}
