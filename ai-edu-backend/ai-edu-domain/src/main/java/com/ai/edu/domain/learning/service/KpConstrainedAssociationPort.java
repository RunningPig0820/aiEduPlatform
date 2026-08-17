package com.ai.edu.domain.learning.service;

import java.util.List;

/**
 * 封闭域约束选择端口——从学段知识点 label 池中选出题目最相关的知识点。
 *
 * <p>从「开放域自由猜测」→「封闭域约束选择」：LLM 只能从池里选，恒返回池内 label
 * （置信低也返回池内最相近，绝不空），消灭空候选死穴 + 跨学段错误（池按学段过滤）。
 */
public interface KpConstrainedAssociationPort {

    /**
     * 从知识点 label 池中选题目最相关的 top-N（≤3）。
     *
     * @param questionText 题目文本
     * @param grade        学生年级（可空）
     * @param pool         学段知识点 label 池（封闭域，LLM 只能从池里选）
     * @return 池内 label top-N；LLM 失败回退池前 N（恒非空）；池空返回空
     */
    List<String> associate(String questionText, Integer grade, List<String> pool);
}
