package com.ai.edu.domain.edukg.model.result;

/**
 * 知识点关联的概念
 */
public record RelatedConcept(
        String conceptUri,
        String conceptLabel
) {
}
