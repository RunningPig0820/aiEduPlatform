package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.learning.model.valueobject.KpResolution;
import com.ai.edu.domain.learning.service.KpDisambiguationPort;
import com.ai.edu.domain.llm.model.AiEduChatRequest;
import com.ai.edu.domain.llm.model.AiEduChatResponse;
import com.ai.edu.domain.llm.service.LlmGateway;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 知识点 LLM 消歧组件——冷启动两段式：LLM 生成候选名 + 镜像校验。
 *
 * <p>题型名（"鸡兔同笼"）与知识点名是两套词汇，靠「知识点名 LIKE 题型名」召回候选是死路，
 * 故冷启动时让 LLM 生成候选知识点名，再回镜像校验（最终 kp 必在镜像，防幻觉）。
 *
 * <p>解析管线③（冷启动消歧）与维护闭环（重判）共用本组件（DRY）。纯消歧，不写 obs。
 */
@Slf4j
@Component
public class KpLlmDisambiguator implements KpDisambiguationPort {

    /** 单候选命中时的置信度（LLM 只提名一个且镜像校验通过；冷启动仍由调用方标 WEAK）。 */
    private static final int SINGLE_CANDIDATE_CONFIDENCE = 70;

    @Resource
    private LlmGateway llmGateway;
    @Resource
    private KgKnowledgePointRepository kgKnowledgePointRepository;

    /**
     * 两段式消歧：LLM 生成候选名 → 镜像校验。
     *
     * @return 单候选 → RESOLVED；多候选 → PENDING 携带候选；零命中/失败 → null
     */
    @Override
    public KpResolution disambiguate(String label, Integer grade) {
        try {
            List<String> names = generateCandidateNames(label, grade);
            if (names.isEmpty()) {
                return null;
            }
            List<KgKnowledgePoint> verified = verifyAgainstMirror(names);
            if (verified.isEmpty()) {
                log.warn("LLM 生成候选名全部未命中镜像: label={}", label);
                return null;
            }
            if (verified.size() == 1) {
                KgKnowledgePoint kp = verified.get(0);
                return KpResolution.resolved(label, kp.getUri(), kp.getLabel(), SINGLE_CANDIDATE_CONFIDENCE);
            }
            List<String> candidates = verified.stream()
                    .map(KgKnowledgePoint::getLabel)
                    .distinct()
                    .toList();
            return KpResolution.pending(label, candidates);
        } catch (Exception e) {
            log.warn("LLM 消歧失败（降级挂起）: label={}", label, e);
            return null;
        }
    }

    /** ① LLM 生成候选知识点名（自由文本，每行一个）。 */
    private List<String> generateCandidateNames(String label, Integer grade) {
        AiEduChatResponse response = llmGateway.chat(AiEduChatRequest.of(buildPrompt(label, grade), 0L)).block();
        if (response == null || response.getResponse() == null || response.getResponse().isBlank()) {
            return List.of();
        }
        return parseNames(response.getResponse());
    }

    private String buildPrompt(String label, Integer grade) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是数学知识点消歧助手。给定题型/知识点「").append(label).append("」");
        if (grade != null) {
            sb.append("（学生年级：").append(grade).append("年级）");
        }
        sb.append("，请列出它最可能对应的 1~5 个教材知识点名，每行一个，只输出知识点名，不要编号、不要解释。");
        return sb.toString();
    }

    /** 解析 LLM 文本为候选名列表（去编号/bullet/空行，去重，限 5 个）。 */
    private List<String> parseNames(String text) {
        return Arrays.stream(text.split("\\R"))
                .map(String::trim)
                .map(s -> s.replaceFirst("^\\s*[0-9]+[.、)）:：]?\\s*", ""))
                .map(s -> s.replaceFirst("^[-*•·]\\s*", ""))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .filter(s -> !s.equalsIgnoreCase("无") && !s.equalsIgnoreCase("没有"))
                .distinct()
                .limit(5)
                .toList();
    }

    /** ② 镜像校验：候选名 exact → LIKE 兜底，命中才保留；去重（同 uri 只留一次）。 */
    private List<KgKnowledgePoint> verifyAgainstMirror(List<String> names) {
        List<KgKnowledgePoint> verified = new ArrayList<>();
        for (String name : names) {
            Optional<KgKnowledgePoint> kp = kgKnowledgePointRepository.findByLabel(name)
                    .filter(k -> k.getUri() != null)
                    .or(() -> kgKnowledgePointRepository.findByLabelLike(name).filter(k -> k.getUri() != null));
            if (kp.isEmpty()) {
                continue;
            }
            String uri = kp.get().getUri();
            if (verified.stream().noneMatch(v -> uri.equals(v.getUri()))) {
                verified.add(kp.get());
            }
        }
        return verified;
    }
}
