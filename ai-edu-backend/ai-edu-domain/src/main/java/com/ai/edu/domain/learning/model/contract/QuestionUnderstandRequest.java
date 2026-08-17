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
 * 图片题目理解请求（Java → Python POST /api/tutoring/question-understand）。
 *
 * <p>Python 视觉模型直接看图（模型 Python 侧写死 TUTORING_DECIDE_MODEL，Java 不指定模型），不经 OCR。
 * tutoring 契约家族统一 snake_case：JSON key 为 {@code image_url}/{@code topic_hint}/{@code grade}。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionUnderstandRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** COS 签名 URL（Python 直接看图） */
    @JsonProperty("image_url")
    private String imageUrl;

    /** 题型名候选提示（传 findTopTopicLabels(20) 收敛命名，可选） */
    @JsonProperty("topic_hint")
    private List<String> topicHint;

    /** 学生年级（年级锚定，可选） */
    private Integer grade;
}
