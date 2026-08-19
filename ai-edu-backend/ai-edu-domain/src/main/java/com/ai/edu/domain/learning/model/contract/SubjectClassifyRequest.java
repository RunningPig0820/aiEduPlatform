package com.ai.edu.domain.learning.model.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 学科分类请求（Java → Python POST /api/tutoring/subject-classify）。
 *
 * <p>学科无关分类器（decide 之前判定题目学科）：只判学科不做解题。
 * tutoring 契约家族统一 snake_case：JSON key 为 {@code image_url}。
 * 契约要点（tutoring-subject-gate design Decision 2）：{@code content} 与 {@code image_url} 至少一个非空。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectClassifyRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 题目文本（与 image_url 至少一个非空，可空） */
    private String content;

    /** 题目图片 COS URL（与 content 至少一个非空，可空） */
    @JsonProperty("image_url")
    private String imageUrl;
}
