package com.ai.edu.domain.learning.model.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Python decide 请求上下文（Java→Python 内部契约，snake_case）。
 *
 * <p><b>不含 current_question</b>——后端不记录题目内容，换题/当前题目由 Python decide
 * 从 {@code history} 语义判断（设计决策 6）。{@code subject_hint} 恒传 math。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecideContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 完整对话历史（Redis 热存，学生消息 + AI 回复） */
    private List<TutoringChatMessage> history;

    /** 护栏计数器：轮次 */
    @JsonProperty("round_count")
    private int roundCount;

    /** 护栏计数器：要答案次数 */
    @JsonProperty("answer_request_count")
    private int answerRequestCount;

    /** 学生掌握度快照（label 必带——Python label 接地，降低 label→URI 解析噪声） */
    @JsonProperty("mastery_snapshot")
    private List<KpSnapshot> masterySnapshot;

    /** 学科提示（本期恒 math） */
    @JsonProperty("subject_hint")
    private String subjectHint;

    /**
     * 换题信号：本轮学生上传了新的题目图片（新 URL 出现）→ true。
     * <p>仅 Java 知道（Java 收到上传、往 history 追加）；Python 见 true 直接返回 type=switch
     * （不调 LLM，省调用且 100% 准）。缺省 false = 正常轮，Python 走 LLM 决策。
     */
    @JsonProperty("is_new_question")
    private boolean isNewQuestion;
}
