package com.ai.edu.domain.learning.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;

/**
 * 派生观测来源（闭集）——标记一条观测是哪个裁判产出的。
 *
 * <p>LLM=主裁判（默认解析/重判）；MIRROR=权威镜像精确/LIKE 命中；CATALOG=题型库年级匹配；
 * CURATED=人工确认；STUDENT_VOTE=学生意图澄清（"你想学哪个"）。
 */
public enum DerivedKpSource implements ValueObject {
    /** 大模型消歧（默认引擎） */
    LLM,
    /** 权威镜像精确/LIKE 命中（确定性规则，0 成本） */
    MIRROR,
    /** 题型库年级匹配（统计先验） */
    CATALOG,
    /** 人工确认/审核 */
    CURATED,
    /** 学生意图澄清（主观意图，软信号） */
    STUDENT_VOTE;

    /** 容错解析：未知或 null 返回 null。 */
    public static DerivedKpSource fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return DerivedKpSource.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
