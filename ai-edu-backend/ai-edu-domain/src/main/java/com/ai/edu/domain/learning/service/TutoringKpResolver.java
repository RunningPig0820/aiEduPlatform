package com.ai.edu.domain.learning.service;

/**
 * 知识点 label → TextbookKP URI 解析端口。
 *
 * <p>在 kg-sync 的 MySQL 镜像（{@code KgKnowledgePointPo}，subject=math）中解析：
 * 精确匹配 → LIKE 模糊 → 未命中返回 null（记日志 + 收尾标记"待收录"，不点亮）。
 * 解析失败不影响答疑主流程。
 *
 * <p>实现位于 Infrastructure 层（{@code TutoringKpResolverImpl}，见任务 8.4）。
 */
public interface TutoringKpResolver {

    /**
     * label → TextbookKP URI。
     *
     * @param label 知识点名（Python mastery_signals / eval 输出的 label）
     * @return TextbookKP URI；未命中返回 {@code null}
     */
    String resolveLabelToUri(String label);
}
