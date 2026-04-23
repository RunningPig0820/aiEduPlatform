package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小节 URI 请求参数
 * 用于查询小节下的知识点列表
 *
 * URI 示例: http://edukg.org/knowledge/3.1/section/math#renjiao-g1s-2-2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionUriRequest {
    /**
     * 小节 URI
     * 格式: http://edukg.org/knowledge/3.1/section/{subject}#{edition}-{grade}-{chapter}-{section}
     * 示例: http://edukg.org/knowledge/3.1/section/math#renjiao-g1s-2-2 (人教版一年级上册第2章第2小节)
     */
    private String sectionUri;
}