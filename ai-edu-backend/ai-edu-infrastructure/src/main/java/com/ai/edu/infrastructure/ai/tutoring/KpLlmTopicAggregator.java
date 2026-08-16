package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.domain.learning.model.valueobject.TopicKpHint;
import com.ai.edu.domain.learning.service.KpTopicAggregationPort;
import com.ai.edu.domain.llm.model.AiEduChatRequest;
import com.ai.edu.domain.llm.model.AiEduChatResponse;
import com.ai.edu.domain.llm.service.LlmGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 题型→知识点 离线 LLM 自动关联组件。
 *
 * <p>离线聚合任务达阈值后调用本组件：LLM 输入「题型名 + 共现知识点分布桶（可能含噪声）」，
 * 输出归一化的题型→知识点占比。最终 kp_uri 必须是输入桶之一（防幻觉），ratio 归一化和=1。
 * 单桶无需归纳、LLM 不可用或解析失败均返回 null，调用方降级纯计数 ratio。
 */
@Slf4j
@Component
public class KpLlmTopicAggregator implements KpTopicAggregationPort {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Resource
    private LlmGateway llmGateway;

    @Override
    public Map<String, Double> refineDistribution(String topicLabel, List<TopicKpHint> hints) {
        if (hints == null || hints.size() <= 1) {
            return null; // 单桶无歧义，无需 LLM 归纳
        }
        try {
            AiEduChatResponse response = llmGateway.chat(AiEduChatRequest.of(buildPrompt(topicLabel, hints), 0L)).block();
            return parseDistribution(response, hints);
        } catch (Exception e) {
            log.warn("LLM 聚合归纳失败（降级纯计数）: topic={}", topicLabel, e);
            return null;
        }
    }

    private String buildPrompt(String topicLabel, List<TopicKpHint> hints) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是数学题型-知识点关联专家。给定题型「").append(topicLabel).append("」，已知其共现知识点分布（可能含噪声）：\n");
        for (TopicKpHint h : hints) {
            sb.append("- ").append(h.getKpLabel() == null ? h.getKpUri() : h.getKpLabel())
              .append("：命中 ").append(h.getHitCount()).append(" 次");
            if (h.getGradeRange() != null) {
                sb.append("，年级 ").append(h.getGradeRange());
            }
            sb.append("（kp_uri=").append(h.getKpUri()).append("）\n");
        }
        sb.append("请判断哪些知识点是真实关联，并给出归一化占比（ratio 和=1，可剔除噪声）。")
          .append("只输出 JSON：{\"distributions\":[{\"kp_uri\":\"<原样回填>\",\"ratio\":0.6}]}，不要解释。");
        return sb.toString();
    }

    /** 解析 LLM JSON，校验 kp_uri 在输入桶内，ratio 归一化和=1。 */
    private Map<String, Double> parseDistribution(AiEduChatResponse response, List<TopicKpHint> hints) {
        if (response == null || response.getResponse() == null || response.getResponse().isBlank()) {
            return null;
        }
        String text = response.getResponse().trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end < start) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(text.substring(start, end + 1));
            JsonNode dists = root.get("distributions");
            if (dists == null || !dists.isArray()) {
                return null;
            }
            Set<String> validUris = hints.stream().map(TopicKpHint::getKpUri).collect(Collectors.toSet());
            Map<String, Double> result = new LinkedHashMap<>();
            double sum = 0.0;
            for (JsonNode d : dists) {
                String uri = d.has("kp_uri") ? d.get("kp_uri").asText() : null;
                double ratio = d.has("ratio") ? d.get("ratio").asDouble() : 0.0;
                if (uri != null && validUris.contains(uri) && ratio > 0) {
                    result.put(uri, ratio);
                    sum += ratio;
                }
            }
            if (result.isEmpty() || sum <= 0) {
                return null;
            }
            double total = sum;
            result.replaceAll((k, v) -> v / total);
            return result;
        } catch (Exception e) {
            log.warn("LLM 聚合响应解析失败: {}", text, e);
            return null;
        }
    }
}
