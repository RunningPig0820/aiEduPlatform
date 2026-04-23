package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识点 URI 请求参数
 * 用于查询知识点详情和知识点图谱
 *
 * URI 示例: http://edukg.org/knowledge/3.1/kp/math#...
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgePointUriRequest {
    /**
     * 知识点 URI
     * 格式: http://edukg.org/knowledge/3.1/kp/{subject}#{...}
     */
    private String kpUri;
}