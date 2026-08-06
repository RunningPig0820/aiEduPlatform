package com.ai.edu.application.assembler.learning;

import com.ai.edu.application.dto.learning.ChatMessageDTO;
import com.ai.edu.application.dto.learning.SummaryDTO;
import com.ai.edu.application.dto.learning.TutoringSessionDTO;
import com.ai.edu.domain.learning.model.contract.MasterySignalItem;
import com.ai.edu.domain.learning.model.entity.TutoringSession;
import com.ai.edu.domain.learning.model.valueobject.MasterySignal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 答疑 DTO ↔ 领域模型转换器。
 */
@Slf4j
@Component
public class TutoringAssembler {

    private static final ObjectMapper SUMMARY_MAPPER = new ObjectMapper();

    /**
     * 领域会话 → 会话响应 DTO（无 questionContent，前端从 recent_messages 推断）。
     */
    public TutoringSessionDTO toSessionDTO(TutoringSession session,
                                           List<ChatMessageDTO> recentMessages,
                                           SummaryDTO summary) {
        return TutoringSessionDTO.builder()
                .sessionId(session.getId())
                .status(session.getStatus() == null ? null : session.getStatus().name())
                .subject(session.getSubject())
                .questionType(session.getQuestionType() == null ? null : session.getQuestionType().name())
                .roundCount(session.getRoundCount())
                .answerRequestCount(session.getAnswerRequestCount())
                .recentMessages(recentMessages)
                .summary(summary)
                .transcriptUrl(session.getTranscriptUrl())
                .build();
    }

    /**
     * Python mastery_signals（DTO）→ 领域掌握度信号。
     * 跳过空/空白 kp_label 的脏信号；signal 由 {@link MasterySignal} 容错转换。
     */
    public List<MasterySignal> toMasterySignals(List<MasterySignalItem> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }
        return dtos.stream()
                .filter(d -> d.getKpLabel() != null && !d.getKpLabel().isBlank())
                .map(d -> MasterySignal.fromCode(d.getKpLabel(), d.getSignal()))
                .toList();
    }

    /**
     * Python action.summary（自由文本/结构化 JSON 字符串）→ 结构化收尾总结 DTO。
     *
     * <p>兼容 knowledgePoints/weakPoints 与 knowledge_points/weak_points 两种键；
     * 解析失败（纯自由文本）返回 null，不阻断（done 事件无 summary 段）。
     */
    public SummaryDTO toSummary(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            JsonNode node = SUMMARY_MAPPER.readTree(raw);
            List<String> kps = readStringList(node, "knowledgePoints", "knowledge_points");
            List<String> wps = readStringList(node, "weakPoints", "weak_points");
            if (kps.isEmpty() && wps.isEmpty()) {
                return null;
            }
            return SummaryDTO.builder().knowledgePoints(kps).weakPoints(wps).build();
        } catch (Exception e) {
            log.warn("[tutoring] action.summary 非结构化文本，忽略: {}", raw);
            return null;
        }
    }

    private List<String> readStringList(JsonNode node, String... keys) {
        List<String> result = new ArrayList<>();
        for (String key : keys) {
            JsonNode arr = node.get(key);
            if (arr != null && arr.isArray()) {
                arr.forEach(n -> {
                    if (n.isTextual() && !n.asText().isBlank()) {
                        result.add(n.asText());
                    }
                });
                break;
            }
        }
        return result;
    }
}
