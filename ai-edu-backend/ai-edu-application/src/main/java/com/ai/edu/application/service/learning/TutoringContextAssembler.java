package com.ai.edu.application.service.learning;

import com.ai.edu.domain.learning.model.contract.DecideContext;
import com.ai.edu.domain.learning.model.contract.GenerateContext;
import com.ai.edu.domain.learning.model.contract.KpSnapshot;
import com.ai.edu.domain.learning.model.contract.TutoringChatMessage;
import com.ai.edu.domain.learning.model.entity.StudentKpMastery;
import com.ai.edu.domain.learning.model.entity.TutoringSession;
import com.ai.edu.domain.learning.model.valueobject.ActionType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 答疑上下文组装器（任务 7.1）——组装 decide / generate 请求上下文。
 *
 * <p><b>不含 current_question</b>——后端零题目状态，换题/当前题目由 Python decide 从 history 语义判断。
 * {@code subject_hint} 恒传 math（本期仅数学）。
 * {@code mastery_snapshot} 必带 {@code label}（Python label 接地，降低 label→URI 解析噪声）。
 */
@Component
public class TutoringContextAssembler {

    public static final String SUBJECT_HINT = "math";

    /**
     * 组装 decide 请求上下文（history 从 Redis 热存、counters 来自会话、掌握度快照带 label）。
     * 缺省换题信号 false（文本/普通轮，向后兼容）。
     */
    public DecideContext buildDecideContext(TutoringSession session,
                                            List<TutoringChatMessage> history,
                                            List<StudentKpMastery> masteryList) {
        return buildDecideContext(session, history, masteryList, false);
    }

    /**
     * 组装 decide 请求上下文（含换题信号）。
     *
     * @param isNewQuestion 本轮学生是否上传了新题目图片（换题信号，见 DecideContext.is_new_question）
     */
    public DecideContext buildDecideContext(TutoringSession session,
                                            List<TutoringChatMessage> history,
                                            List<StudentKpMastery> masteryList,
                                            boolean isNewQuestion) {
        return DecideContext.builder()
                .history(history == null ? List.of() : history)
                .roundCount(session.getRoundCount())
                .answerRequestCount(session.getAnswerRequestCount())
                .masterySnapshot(toKpSnapshot(masteryList))
                .subjectHint(SUBJECT_HINT)
                .isNewQuestion(isNewQuestion)
                .build();
    }

    /**
     * 组装 generate 请求上下文：history + 已放行的 type（可能异于 decide 原始输出）+ 原 action 元数据。
     */
    public GenerateContext buildGenerateContext(List<TutoringChatMessage> history,
                                                ActionType allowedType,
                                                com.ai.edu.domain.learning.model.contract.ActionMeta actionMeta) {
        return GenerateContext.builder()
                .history(history == null ? List.of() : history)
                .subjectHint(SUBJECT_HINT)
                .actionType(allowedType.name().toLowerCase())
                .actionMeta(actionMeta)
                .build();
    }

    /**
     * 学生掌握度实体 → 快照项（kp_key=TextbookKP URI、label 必带、mastery_level 分值）。
     */
    public List<KpSnapshot> toKpSnapshot(List<StudentKpMastery> masteryList) {
        if (masteryList == null || masteryList.isEmpty()) {
            return List.of();
        }
        return masteryList.stream()
                .filter(m -> m.getKpKey() != null)
                .map(m -> KpSnapshot.builder()
                        .kpKey(m.getKpKey().getValue())
                        .label(m.getKpLabel())
                        .masteryLevel(m.getMasteryLevel() == null ? 0 : m.getMasteryLevel().getValue())
                        .build())
                .toList();
    }
}
