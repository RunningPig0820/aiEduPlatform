package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import com.ai.edu.domain.learning.service.QuestionUnderstandingPort;
import com.ai.edu.domain.llm.model.AiEduChatRequest;
import com.ai.edu.domain.llm.model.AiEduChatResponse;
import com.ai.edu.domain.llm.service.LlmGateway;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 题目理解默认实现——LLM 识别题型名 + 题型库参考词表收敛命名。
 *
 * <p>prompt 注入题型库 top-20 常用题型名作参考词表（findTopTopicLabels），
 * 让 LLM 题型命名偏向现有词汇，从源头降低变体漂移（别名合并之外的第一道防线，纯 prompt 零成本）。
 * 解析复用 {@link KpLlmDisambiguator#parseNames} 去编号/bullet。LLM 失败/空返回空列表（调用方降级 PENDING）。
 *
 * <p>纯识别不落库：题型名后续由解析管线 resolve（镜像 → 题型库 → LLM 消歧）桥接到 TextbookKP URI。
 * 端口预留 Python 独立端点实现（拆 decide 题目理解），换实现只动装配。
 */
@Slf4j
@Component
public class KpQuestionAnalyzer implements QuestionUnderstandingPort {

    private static final int VOCABULARY_LIMIT = 20;

    @Resource
    private LlmGateway llmGateway;
    @Resource
    private QuestionTypeRepository questionTypeRepository;

    @Override
    public List<String> understand(String questionText, Integer grade) {
        try {
            List<String> known = questionTypeRepository.findTopTopicLabels(VOCABULARY_LIMIT);
            AiEduChatResponse response = llmGateway.chat(
                    AiEduChatRequest.of(buildPrompt(questionText, grade, known), 0L)).block();
            if (response == null || response.getResponse() == null || response.getResponse().isBlank()) {
                return List.of();
            }
            return KpLlmDisambiguator.parseNames(response.getResponse());
        } catch (Exception e) {
            log.warn("题目理解失败（降级空候选）: text={}", truncate(questionText), e);
            return List.of();
        }
    }

    private String buildPrompt(String questionText, Integer grade, List<String> known) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是数学题型识别助手。识别下面这道题最可能属于的题型名。");
        if (grade != null) {
            sb.append("（学生年级：").append(grade).append("年级）");
        }
        if (!known.isEmpty()) {
            // 参考词表强制优先：同文本命名收敛，避免每次候选叫法漂移（确定性靠提示词，不靠缓存）
            sb.append("\n已知题型库常用题型（必须优先从中选取，仅当明显不匹配时才自拟，避免自造新叫法）：");
            for (String label : known) {
                sb.append("\n- ").append(label);
            }
        }
        sb.append("\n要求：\n")
                .append("1. 按可能性从高到低列出候选题型名，每行一个，最多 5 个；\n")
                .append("2. 只输出题型名，不要编号、不要解释；\n")
                .append("3. 若题目不属于任何已知题型，输出：无法识别\n")
                .append("题目：\n").append(questionText);
        return sb.toString();
    }

    private String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 80 ? s : s.substring(0, 80) + "…";
    }
}
