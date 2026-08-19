package com.ai.edu.application.service.learning;

import com.ai.edu.domain.learning.model.contract.DecideContext;
import com.ai.edu.domain.learning.model.contract.GenerateContext;
import com.ai.edu.domain.learning.model.contract.KpSnapshot;
import com.ai.edu.domain.learning.model.contract.MasterySignalItem;
import com.ai.edu.domain.learning.model.contract.TutoringChatMessage;
import com.ai.edu.domain.learning.model.entity.StudentTopicMastery;
import com.ai.edu.domain.learning.model.entity.TutoringSession;
import com.ai.edu.domain.learning.model.valueobject.ActionType;
import com.ai.edu.domain.learning.model.valueobject.MasteryLevel;
import com.ai.edu.domain.learning.model.valueobject.TopicKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 答疑上下文组装器测试（task 7.1：decide 上下文组装，不含 current_question，mastery_snapshot 带 label）。
 */
class TutoringContextAssemblerTest {

    private final TutoringContextAssembler assembler = new TutoringContextAssembler();

    @Test
    @DisplayName("buildDecideContext 组装 counters + subject_hint=math + 掌握度快照带 label")
    void buildDecideContext_shouldAssemble() {
        TutoringSession session = TutoringSession.start(501L, "math");
        session.setId(1001L);
        session.recordRound();
        session.recordRound(); // round=2
        session.requestAnswer(); // count=1
        List<TutoringChatMessage> history = List.of(
                TutoringChatMessage.user("鸡兔同笼"),
                TutoringChatMessage.ai("先找已知条件"));

        StudentTopicMastery mastery = StudentTopicMastery.restore(
                1L, 501L, TopicKey.of("鸡兔同笼"), "鸡兔同笼", MasteryLevel.of(64), "ai", 3L, null);

        DecideContext ctx = assembler.buildDecideContext(session, history, List.of(mastery));

        assertEquals(2, ctx.getHistory().size());
        assertEquals("user", ctx.getHistory().get(0).getRole());
        assertEquals(2, ctx.getRoundCount());
        assertEquals(1, ctx.getAnswerRequestCount());
        assertEquals("math", ctx.getSubjectHint());
        assertEquals(1, ctx.getMasterySnapshot().size());
        KpSnapshot snapshot = ctx.getMasterySnapshot().get(0);
        assertEquals("鸡兔同笼", snapshot.getKpKey()); // kp_key 承载题型 key（学生掌握题型）
        assertEquals("鸡兔同笼", snapshot.getLabel()); // label 必带（Python label 接地）
        assertEquals(64, snapshot.getMasteryLevel());
    }

    @Test
    @DisplayName("buildDecideContext 空掌握度列表 → 空快照，不报错")
    void buildDecideContext_emptyMastery() {
        TutoringSession session = TutoringSession.start(501L, "math");
        DecideContext ctx = assembler.buildDecideContext(session, null, null);
        assertTrue(ctx.getHistory().isEmpty());
        assertTrue(ctx.getMasterySnapshot().isEmpty());
        assertEquals("math", ctx.getSubjectHint());
    }

    @Test
    @DisplayName("buildDecideContext 换题信号透传：4 参置 true / 3 参缺省 false")
    void buildDecideContext_isNewQuestion() {
        TutoringSession session = TutoringSession.start(501L, "math");
        session.setId(1001L);
        List<TutoringChatMessage> history = List.of(
                TutoringChatMessage.user("鸡兔同笼"), TutoringChatMessage.ai("先找已知条件"));

        DecideContext withSignal = assembler.buildDecideContext(session, history, null, true);
        DecideContext defaultCtx = assembler.buildDecideContext(session, history, null);

        assertTrue(withSignal.isNewQuestion(), "新图上传轮 is_new_question 应为 true");
        assertFalse(defaultCtx.isNewQuestion(), "缺省轮 is_new_question 应为 false");
    }

    @Test
    @DisplayName("buildGenerateContext 用已放行 type（小写）+ 原 action 元数据")
    void buildGenerateContext_usesAllowedType() {
        List<TutoringChatMessage> history = List.of(TutoringChatMessage.user("设鸡x只"));
        com.ai.edu.domain.learning.model.contract.ActionMeta meta =
                com.ai.edu.domain.learning.model.contract.ActionMeta.builder()
                        .type("reveal").build();

        GenerateContext ctx = assembler.buildGenerateContext(history, ActionType.APPROACH, meta);

        assertEquals("approach", ctx.getActionType()); // 已放行 type 替代原始 reveal
        assertEquals(meta, ctx.getActionMeta());
        assertEquals("math", ctx.getSubjectHint());
    }

    @Test
    @DisplayName("toKpSnapshot 跳过无题型 key 的脏记录")
    void toKpSnapshot_skipDirty() {
        TutoringSession session = TutoringSession.start(501L, "math");
        StudentTopicMastery dirty = StudentTopicMastery.restore(
                1L, 501L, null, "无key", MasteryLevel.of(0), "ai", 0L, null);
        StudentTopicMastery valid = StudentTopicMastery.restore(
                2L, 501L, TopicKey.of("有理数"), "有理数", MasteryLevel.of(75), "ai", 5L, null);

        List<KpSnapshot> snapshots = assembler.toKpSnapshot(List.of(dirty, valid));

        assertEquals(1, snapshots.size());
        assertEquals(75, snapshots.get(0).getMasteryLevel());
        assertEquals("有理数", snapshots.get(0).getLabel());
    }
}
