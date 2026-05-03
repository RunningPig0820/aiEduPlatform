package com.ai.edu.domain.edukg.model.result;

import java.util.List;

/**
 * 节点展开查询结果（单条邻居关系）
 */
public record ExpandRelationResult(
        List<String> sourceLabels,
        String sourceLabel,
        String relType,
        boolean isOutgoing,
        List<String> targetLabels,
        String targetUri,
        String targetLabel
) {
}
