package com.ai.edu.domain.learning.model.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 题型名向量 metadata（put/query 共享）。tutoring 契约家族统一 snake_case。
 *
 * <p>python-integration 契约：metadata 固定承载 student_id / topic_label / canonical_label / timestamp，
 * 供向量桶检索结果追溯「谁、什么题型、归到哪个 canonical」。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicVectorMetadata implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 学生 ID（来源题主） */
    @JsonProperty("student_id")
    private String studentId;

    /** LLM 原始题型名 */
    @JsonProperty("topic_label")
    private String topicLabel;

    /** 聚集后 canonical 题型名 */
    @JsonProperty("canonical_label")
    private String canonicalLabel;

    /** 落库时间（ISO-8601） */
    private String timestamp;
}
