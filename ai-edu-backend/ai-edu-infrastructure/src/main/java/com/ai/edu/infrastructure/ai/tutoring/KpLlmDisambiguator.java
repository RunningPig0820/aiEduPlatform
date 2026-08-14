package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.learning.model.valueobject.KpResolution;
import com.ai.edu.domain.learning.service.KpDisambiguationPort;
import com.ai.edu.domain.llm.model.AiEduChatRequest;
import com.ai.edu.domain.llm.model.AiEduChatResponse;
import com.ai.edu.domain.llm.service.LlmGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 知识点 LLM 消歧组件——给定题型 label + 学生年级，从候选知识点中选最匹配（选择题 + 候选内校验防幻觉）。
 *
 * <p>解析管线③（冷启动消歧）与维护闭环（重判）共用本组件（DRY）。纯消歧，不写 obs。
 */
@Slf4j
@Component
public class KpLlmDisambiguator implements KpDisambiguationPort {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Resource
    private LlmGateway llmGateway;
    @Resource
    private KgKnowledgePointRepository kgKnowledgePointRepository;

    /**
     * LLM 消歧：label + 年级 → 候选（镜像 LIKE 召回）→ 选择题 → 候选内校验。
     *
     * @return 解析结果（RESOLVED + 置信度）；失败/无候选返回 null（降级挂起）
     */
    @Override
    public KpResolution disambiguate(String label, Integer grade) {
        List<KgKnowledgePoint> candidates = kgKnowledgePointRepository.findByLabelLikeList(label);
        if (candidates.isEmpty()) {
            return null;
        }
        try {
            String prompt = buildDisambiguationPrompt(label, grade, candidates);
            AiEduChatResponse response = llmGateway.chat(AiEduChatRequest.of(prompt, 0L)).block();
            return parseDisambiguationResponse(label, response, candidates);
        } catch (Exception e) {
            log.warn("LLM 消歧失败（降级挂起）: label={}", label, e);
            return null;
        }
    }

    private String buildDisambiguationPrompt(String label, Integer grade, List<KgKnowledgePoint> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是数学知识点消歧助手。给定题型/知识点「").append(label).append("」");
        if (grade != null) {
            sb.append("（学生年级：").append(grade).append("年级）");
        }
        sb.append("，从下列候选知识点中选出最匹配的一个，并给出置信度 0-100。\n\n候选：\n");
        for (int i = 0; i < candidates.size(); i++) {
            KgKnowledgePoint c = candidates.get(i);
            sb.append(i + 1).append(". ").append(c.getLabel()).append(" (").append(c.getUri()).append(")\n");
        }
        sb.append("\n只输出 JSON，格式：{\"kp_uri\":\"<候选中的 uri>\",\"confidence\":<0-100>}。");
        return sb.toString();
    }

    private KpResolution parseDisambiguationResponse(String label, AiEduChatResponse response,
                                                     List<KgKnowledgePoint> candidates) {
        if (response == null || response.getResponse() == null || response.getResponse().isBlank()) {
            return null;
        }
        String text = response.getResponse().trim();
        int jsonStart = text.indexOf('{');
        int jsonEnd = text.lastIndexOf('}');
        if (jsonStart < 0 || jsonEnd < jsonStart) {
            return null;
        }
        String json = text.substring(jsonStart, jsonEnd + 1);
        try {
            JsonNode node = MAPPER.readTree(json);
            String uri = node.has("kp_uri") ? node.get("kp_uri").asText() : null;
            int confidence = node.has("confidence") ? node.get("confidence").asInt() : 0;
            if (uri == null || uri.isBlank()) {
                return null;
            }
            // 候选内校验：LLM 只做选择题，返回候选外 uri 视为幻觉，忽略
            Optional<KgKnowledgePoint> matched = candidates.stream()
                    .filter(c -> uri.equals(c.getUri()))
                    .findFirst();
            if (matched.isEmpty()) {
                log.warn("LLM 消歧返回候选外 uri，忽略: {}", uri);
                return null;
            }
            int clamped = Math.min(100, Math.max(0, confidence));
            return KpResolution.resolved(label, uri, matched.get().getLabel(), clamped);
        } catch (Exception e) {
            log.warn("LLM 消歧响应解析失败: {}", text, e);
            return null;
        }
    }
}
