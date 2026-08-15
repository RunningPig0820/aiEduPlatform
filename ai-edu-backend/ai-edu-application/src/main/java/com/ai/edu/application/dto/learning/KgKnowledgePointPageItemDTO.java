package com.ai.edu.application.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识点总览分页条目（POST /api/kg/knowledge-points）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KgKnowledgePointPageItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 知识点 URI */
    private String kpUri;

    /** 知识点名 */
    private String kpLabel;

    /** 学段 primary/middle/high */
    private String stage;

    /** 归属章节名（无归属为 null） */
    private String chapterLabel;

    /** 归属小节名（无归属为 null） */
    private String sectionLabel;
}
