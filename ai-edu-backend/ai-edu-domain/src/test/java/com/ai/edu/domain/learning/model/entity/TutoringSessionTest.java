package com.ai.edu.domain.learning.model.entity;

import com.ai.edu.domain.learning.model.valueobject.EndReason;
import com.ai.edu.domain.learning.model.valueobject.TutoringConstants;
import com.ai.edu.domain.learning.model.valueobject.TutoringEmotion;
import com.ai.edu.domain.learning.model.valueobject.TutoringState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TutoringSession 聚合根领域单测（test.md 领域单测一节）。
 */
class TutoringSessionTest {

    @Test
    @DisplayName("start() 应创建 ACTIVE 会话，计数器归零，subject 默认 math")
    void start_shouldCreateActiveSession() {
        TutoringSession session = TutoringSession.start(1001L, null);
        assertEquals(TutoringState.ACTIVE, session.getStatus());
        assertEquals(1001L, session.getStudentId());
        assertEquals("math", session.getSubject());
        assertEquals(0, session.getRoundCount());
        assertEquals(0, session.getAnswerRequestCount());
        assertNull(session.getId());
        assertTrue(session.isActive());
    }

    @Test
    @DisplayName("recordRound() 递增轮次，达上限后抛 TutoringRoundLimitException")
    void recordRound_reachesLimit_shouldThrow() {
        TutoringSession session = TutoringSession.start(1001L, "math");
        for (int i = 0; i < TutoringConstants.SESSION_ROUND_LIMIT; i++) {
            session.recordRound();
        }
        assertEquals(TutoringConstants.SESSION_ROUND_LIMIT, session.getRoundCount());
        assertThrows(TutoringRoundLimitException.class, session::recordRound);
    }

    @Test
    @DisplayName("requestAnswer() 返回第几次并递增")
    void requestAnswer_shouldReturnIncrementingCount() {
        TutoringSession session = TutoringSession.start(1001L, "math");
        assertEquals(1, session.requestAnswer());
        assertEquals(2, session.requestAnswer());
        assertEquals(2, session.getAnswerRequestCount());
    }

    @Test
    @DisplayName("switchQuestion() 重置轮次与要答案计数（不记录题目）")
    void switchQuestion_shouldResetCounters() {
        TutoringSession session = TutoringSession.start(1001L, "math");
        session.recordRound();
        session.recordRound();
        session.requestAnswer();
        session.switchQuestion();
        assertEquals(0, session.getRoundCount());
        assertEquals(0, session.getAnswerRequestCount());
    }

    @Test
    @DisplayName("complete() 置 ARCHIVED + endReason + archivedAt")
    void complete_shouldArchive() {
        TutoringSession session = TutoringSession.start(1001L, "math");
        session.complete(EndReason.COMPLETED);
        assertEquals(TutoringState.ARCHIVED, session.getStatus());
        assertEquals(EndReason.COMPLETED, session.getEndReason());
        assertNotNull(session.getArchivedAt());
        assertFalse(session.isActive());
    }

    @Test
    @DisplayName("terminate() 置 TERMINATED + endReason（可空）")
    void terminate_shouldTerminate() {
        TutoringSession session = TutoringSession.start(1001L, "math");
        session.terminate(null);
        assertEquals(TutoringState.TERMINATED, session.getStatus());
        assertNull(session.getEndReason());

        TutoringSession roundLimit = TutoringSession.start(1001L, "math");
        roundLimit.terminate(EndReason.ROUND_LIMIT);
        assertEquals(EndReason.ROUND_LIMIT, roundLimit.getEndReason());
    }

    @Test
    @DisplayName("已结束会话不能 recordRound / 重复收尾")
    void endedSession_shouldRejectMutatingOps() {
        TutoringSession session = TutoringSession.start(1001L, "math");
        session.complete(EndReason.COMPLETED);
        assertThrows(IllegalStateException.class, session::recordRound);
        assertThrows(IllegalStateException.class, () -> session.complete(EndReason.ABANDONED));
        assertThrows(IllegalStateException.class, () -> session.terminate(null));
    }

    @Test
    @DisplayName("setLastEmotion/setKpClassification/updateTranscriptUrl 基础设置")
    void setters_shouldWork() {
        TutoringSession session = TutoringSession.start(1001L, "math");
        session.setLastEmotion(TutoringEmotion.CONFUSED);
        assertEquals(TutoringEmotion.CONFUSED, session.getLastEmotion());
        session.setLastEmotion(null);
        assertEquals(TutoringEmotion.NEUTRAL, session.getLastEmotion());
        session.updateTranscriptUrl("tutoring/transcripts/1.json");
        assertEquals("tutoring/transcripts/1.json", session.getTranscriptUrl());
        assertNotNull(session.getUpdatedAt());
    }
}
