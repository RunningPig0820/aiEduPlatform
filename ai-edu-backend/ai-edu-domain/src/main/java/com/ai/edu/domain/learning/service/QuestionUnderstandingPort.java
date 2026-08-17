package com.ai.edu.domain.learning.service;

import java.util.List;

/**
 * 题目理解端口——题目文本 → 候选题型名（LLM 题目理解）。
 *
 * <p>纯识别，不查库不落库；供 analyze-question 独立入口与答疑复用。
 * 默认实现 {@code KpQuestionAnalyzer}（infra，LLM + 题型库参考词表）；
 * Python 独立端点（拆 decide 题目理解）作为可替换实现——换实现只动 infra 装配，domain/application 不变。
 */
public interface QuestionUnderstandingPort {

    /**
     * 题目文本 → 候选题型名（LLM 识别，限 1~5 个）。
     *
     * @param questionText 题目文本
     * @param grade        学生年级（可空，无年级锚降级）
     * @return 候选题型名列表；LLM 失败/空返回空列表（调用方降级 PENDING）
     */
    List<String> understand(String questionText, Integer grade);
}
