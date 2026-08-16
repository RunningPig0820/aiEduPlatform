package com.ai.edu.domain.learning.service;

import com.ai.edu.domain.learning.model.valueobject.TopicKpHint;

import java.util.List;
import java.util.Map;

/**
 * 题型→知识点 离线 LLM 自动关联端口（题型库自我生长的离线一环）。
 *
 * <p>给定题型名 + 共现知识点分布桶，LLM 判断真实关联并输出归一化占比。
 * 实现位于 Infrastructure 层（{@code KpLlmTopicAggregator}）。
 */
public interface KpTopicAggregationPort {

    /**
     * LLM 归纳题型→知识点分布。
     *
     * @param topicLabel 题型名
     * @param hints      共现知识点分布桶（kp_uri + label + 命中 + 年级段）
     * @return 归一化后的 kpUri → ratio 映射（和=1）；单桶 / LLM 不可用 / 解析失败返回 null（降级纯计数 ratio）
     */
    Map<String, Double> refineDistribution(String topicLabel, List<TopicKpHint> hints);
}
