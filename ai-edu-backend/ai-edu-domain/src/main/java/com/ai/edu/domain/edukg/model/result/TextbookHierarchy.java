package com.ai.edu.domain.edukg.model.result;

/**
 * 教材层级路径：Textbook -> Chapter -> Section
 */
public record TextbookHierarchy(
        String textbookUri,
        String textbookLabel,
        String chapterUri,
        String chapterLabel,
        String sectionUri,
        String sectionLabel
) {
}
