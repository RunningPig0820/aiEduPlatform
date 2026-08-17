package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.domain.learning.service.KpConstrainedAssociationPort;
import com.ai.edu.domain.llm.model.AiEduChatRequest;
import com.ai.edu.domain.llm.model.AiEduChatResponse;
import com.ai.edu.domain.llm.service.LlmGateway;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 封闭域约束选择默认实现——LLM 只能从学段知识点池里选最相关 top-N（恒非空）。
 *
 * <p>从「开放域自由猜测」→「封闭域约束选择」：prompt 强制「必须从池里选，禁止池外，禁止说无法确定」；
 * 输出经 parseNames 解析 + 池内过滤（跨学段/幻觉 label 被挡）；LLM 失败/空 → 回退池前 N（确定性兜底，恒非空）。
 * 复用 {@link KpLlmDisambiguator#parseNames} 去编号/bullet。
 */
@Slf4j
@Component
public class KpConstrainedAssociator implements KpConstrainedAssociationPort {

    private static final int TOP_N = 3;

    @Resource
    private LlmGateway llmGateway;

    @Override
    public List<String> associate(String questionText, Integer grade, List<String> pool) {
        if (pool == null || pool.isEmpty()) {
            return List.of();
        }
        try {
            AiEduChatResponse response = llmGateway.chat(
                    AiEduChatRequest.of(buildPrompt(questionText, grade, pool), 0L)).block();
            if (response == null || response.getResponse() == null || response.getResponse().isBlank()) {
                return fallback(pool);
            }
            List<String> selected = KpLlmDisambiguator.parseNames(response.getResponse()).stream()
                    .filter(pool::contains) // 强制池内，池外 label 丢弃
                    .distinct()
                    .limit(TOP_N)
                    // 池内排序确定性：同文本 + 同池 → 结果顺序稳定（LLM 顺序打乱不影响 top-1）
                    .sorted(Comparator.comparingInt(pool::indexOf))
                    .toList();
            return selected.isEmpty() ? fallback(pool) : selected;
        } catch (Exception e) {
            log.warn("约束选择失败（回退池前 N，恒非空）: text={}", truncate(questionText), e);
            return fallback(pool);
        }
    }

    /** 确定性兜底：LLM 失败/全池外时取池前 N，保证恒非空。 */
    private List<String> fallback(List<String> pool) {
        return pool.stream().limit(TOP_N).toList();
    }

    private String buildPrompt(String questionText, Integer grade, List<String> pool) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是数学知识点关联助手。识别下面这道题最可能涉及的知识点。");
        if (grade != null) {
            sb.append("（学生年级：").append(grade).append("年级）");
        }
        sb.append("\n要求：从下面的知识点池中选出最相关的 1~3 个，每行一个；")
                .append("必须从池里选，禁止输出池外内容，禁止说无法确定/没有相关知识点。");
        sb.append("\n知识点池：");
        for (String label : pool) {
            sb.append("\n- ").append(label);
        }
        sb.append("\n题目：\n").append(questionText);
        return sb.toString();
    }

    private String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 80 ? s : s.substring(0, 80) + "…";
    }
}
