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
 * 图片题目理解结果（Python → Java）。
 *
 * <p>tutoring 契约家族统一 snake_case：JSON key 为 {@code topic_labels}/{@code question_kps}。
 * topicLabels 空 = 识别失败，调用方降级 PENDING（不报错）；questionKps 为 LLM 顺带参考知识点（不强求）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionUnderstandResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 识别出的题型名列表（可能多个候选） */
    @JsonProperty("topic_labels")
    private List<String> topicLabels;

    /** LLM 顺带判断的知识点名（不强求，可空） */
    @JsonProperty("question_kps")
    private List<String> questionKps;

    /** 识别失败（topicLabels 空）→ 调用方降级 PENDING。 */
    public boolean isFailed() {
        return topicLabels == null || topicLabels.isEmpty();
    }
}
