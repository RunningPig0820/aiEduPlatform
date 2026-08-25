package com.ai.edu.infrastructure.ai.rag;

import com.ai.edu.domain.learning.model.contract.RagQualityScore;
import com.ai.edu.domain.learning.service.RagQualityGrader;
import com.ai.edu.domain.llm.model.AiEduChatRequest;
import com.ai.edu.domain.llm.model.AiEduChatResponse;
import com.ai.edu.domain.llm.service.LlmGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * RAG 回答质量评审实现：复用 {@link LlmGateway#chat}（LLM 网关）打分 0-5。
 *
 * <p>异步调用（boundedElastic），打分失败返回 empty（不入累计，不打断问答链路）。
 * 评分 prompt 要求结构化 JSON {score, reason}，宽容解析。
 */
@Slf4j
@Repository
public class RagQualityGraderImpl implements RagQualityGrader {

    private static final String SCENE = "rag_quality_grade";
    private static final Long SYSTEM_USER_ID = 0L; // Python ChatRequest.user_id 必填 int，系统打分用哨兵
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    /** 引用片段摘要拼接上限（字符），防止 prompt 暴涨触发 LLM 入参超限 */
    private static final int SUMMARIES_MAX_CHARS = 800;
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Resource
    private LlmGateway llmGateway;

    @Override
    public Mono<RagQualityScore> grade(String question, String answer, java.util.List<String> quotedKeys,
                                       java.util.List<String> blockSummaries) {
        String prompt = buildPrompt(question, answer, quotedKeys, blockSummaries);
        return llmGateway.chat(AiEduChatRequest.of(prompt, SYSTEM_USER_ID, SCENE))
                .timeout(TIMEOUT)
                .map(AiEduChatResponse::getResponse)
                .map(this::parseScore)
                .doOnNext(s -> log.info("[rag-quality] 评分完成: score={}, reason={}", s.getScore(), s.getReason()))
                .onErrorResume(e -> {
                    log.warn("[rag-quality] 评分失败（不入累计）: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private String buildPrompt(String question, String answer, java.util.List<String> quotedKeys,
                               java.util.List<String> blockSummaries) {
        String keys = (quotedKeys == null || quotedKeys.isEmpty()) ? "（无）" : String.join("、", quotedKeys);
        return """
                你是RAG问答质量评审员，严格按照下面4个维度对助手答案进行0-5整数打分。
                评分维度定义：
                【相关性】：答案是否针对用户问题，不答非所问。
                【完整性】：是否覆盖问题需要的关键信息，无明显遗漏。
                【忠实度】：答案内容必须严格依据引用知识，禁止编造不存在信息；存在编造、幻觉、来源无依据，必须大幅降分。
                【清晰度】：表达通顺，逻辑易懂。

                打分标尺：
                5分：全部维度优秀，忠实无幻觉，回答完整贴合问题。
                4分：整体良好，微小瑕疵，无幻觉。
                3分：基本合格，存在小缺陷，无严重幻觉。
                2分：存在明显缺陷，信息缺失或轻微编造。
                1分：大部分内容不可用。
                0分：完全答非所问，严重幻觉。

                打分规则：
                1.忠实度优先级最高，如果答案出现引用无法支撑的编造内容，总分最高不超过2分。
                2.多个缺陷同时存在，按最严重缺陷优先降档，再叠加其他缺陷扣分。
                3.若答案与引用知识库片段主题完全无关，即使行文通顺，也不得给到4-5分。

                学生问题：%s
                助手答案：%s
                答案引用的知识块id：%s
                引用知识库片段摘要：
                %s

                特殊规则：当【无引用片段】时，没有知识库来源核对忠实度，忠实度维度不扣分，仅评估相关性、完整性、清晰度。

                只返回JSON，禁止输出其他解释文本。
                格式：{"score": <0-5整数>, "reason": "<一句话评审，描述主要缺陷或优点>"}
                """.formatted(
                question,
                answer,
                keys,
                formatSummaries(blockSummaries));
    }

    /** 片段摘要拼接 + 按【整块】截断保护（不硬切，避免把支撑答案的关键句截断）；无摘要 → 明示无引用片段。 */
    private String formatSummaries(java.util.List<String> blockSummaries) {
        if (blockSummaries == null || blockSummaries.isEmpty()) {
            return "（无引用片段）";
        }
        StringBuilder sb = new StringBuilder();
        for (String summary : blockSummaries) {
            // 已有一块时再加会超预算 → 停止，整块保留
            if (sb.length() > 0 && sb.length() + summary.length() + 1 > SUMMARIES_MAX_CHARS) {
                sb.append("\n…（其余片段已省略）");
                return sb.toString();
            }
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(summary);
        }
        return sb.toString();
    }

    /** 解析 LLM JSON 响应 → RagQualityScore；非 JSON/越界 → 抛异常走兜底 empty。 */
    private RagQualityScore parseScore(String response) {
        String text = response == null ? "" : response.trim();
        // 容忍 LLM 偶尔在 JSON 外套代码块/说明
        int braceStart = text.indexOf('{');
        int braceEnd = text.lastIndexOf('}');
        if (braceStart >= 0 && braceEnd > braceStart) {
            text = text.substring(braceStart, braceEnd + 1);
        }
        try {
            JsonNode node = MAPPER.readTree(text);
            int score = node.path("score").asInt(-1);
            if (score < 0 || score > 5) {
                throw new IllegalStateException("评分越界: " + score);
            }
            return RagQualityScore.builder()
                    .score(score)
                    .reason(node.path("reason").asText(""))
                    .build();
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("评分响应非 JSON", e);
        }
    }
}
