package com.ai.edu.domain.learning.model.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 题型名向量 put 请求（Java → Python 向量端点）。
 *
 * <p>tutoring 契约家族统一 snake_case：JSON key 为 {@code vector_type}。
 * {@code vectorType} 由桥实现（{@code TopicVectorClient}）强制为 {@code "topic"}——Python 契约
 * `vector_type` 必填路由键、无缺省，本期唯一合法值。
 */
@Getter
@Builder
public class TopicVectorPutRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 向量 key（幂等 upsert，覆盖） */
    private String key;

    /** 题型名（本期只传题型名向量，题目向量不落库） */
    private String text;

    /** 向量类型路由键（恒 "topic"，Python 无缺省；未知 → 400） */
    @JsonProperty("vector_type")
    private String vectorType;

    /** 追溯 metadata（student_id / topic_label / canonical_label / timestamp） */
    private TopicVectorMetadata metadata;
}
