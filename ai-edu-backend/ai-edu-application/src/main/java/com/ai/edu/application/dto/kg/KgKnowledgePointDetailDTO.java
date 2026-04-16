package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识点详情 DTO（含 2 层父级：小节 + 章节）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KgKnowledgePointDetailDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String uri;
    private String label;
    private String difficulty;
    private String importance;
    private String cognitiveLevel;

    // 父级：小节
    private String sectionUri;
    private String sectionLabel;

    // 爷爷级：章节
    private String chapterUri;
    private String chapterLabel;
}
