package com.ai.edu.application.dto.learning;

import lombok.Data;

/**
 * 知识点总览分页请求（POST /api/kg/knowledge-points）。
 */
@Data
public class KgKnowledgePointPageRequest {

    /** 学段 primary/middle/high */
    private String stage;

    /** 页码（从 1 起） */
    private Integer page;

    /** 每页条数 */
    private Integer size;

    /** 搜索关键词（kp.label LIKE %keyword%，可选；空则返回该学段全量） */
    private String keyword;
}
