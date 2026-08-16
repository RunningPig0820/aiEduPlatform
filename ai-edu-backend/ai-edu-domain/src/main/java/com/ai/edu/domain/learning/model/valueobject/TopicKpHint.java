package com.ai.edu.domain.learning.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 题型→知识点 共现提示（离线 LLM 聚合的输入桶）。
 *
 * <p>离线聚合任务把某题型累积的 obs 按 kp 拆分，得到该题型的知识点共现桶，
 * 作为 LLM 归纳「题型→知识点可靠映射」的输入。
 */
@Getter
@Builder
@AllArgsConstructor
public class TopicKpHint {

    /** 知识点 URI */
    private final String kpUri;

    /** 知识点名（kg 镜像反查，供 LLM 语义判断） */
    private final String kpLabel;

    /** 该桶命中次数 */
    private final int hitCount;

    /** 该桶覆盖年级段（如 4-6，无年级锚为 null） */
    private final String gradeRange;
}
