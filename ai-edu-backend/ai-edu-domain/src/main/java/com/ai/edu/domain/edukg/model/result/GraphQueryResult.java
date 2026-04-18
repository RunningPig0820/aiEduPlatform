package com.ai.edu.domain.edukg.model.result;

import java.util.List;

/**
 * 图谱关系查询结果
 */
public record GraphQueryResult(
        String kpUri,
        String kpLabel,
        String kpDifficulty,
        String kpCognitiveLevel,
        String kpImportance,
        List<TextbookHierarchy> hierarchies,
        List<RelatedConcept> relatedConcepts
) {

    public static GraphQueryResult empty(String kpUri) {
        return new GraphQueryResult(kpUri, null, null, null, null, List.of(), List.of());
    }
}
