package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 教材 URI 请求参数
 * 用于查询教材下的章节树
 *
 * URI 示例: http://edukg.org/knowledge/3.1/textbook/math#renjiao-g1s
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextbookUriRequest {
    /**
     * 教材 URI
     * 格式: http://edukg.org/knowledge/3.1/textbook/{subject}#{edition}-{grade}
     * 示例: http://edukg.org/knowledge/3.1/textbook/math#renjiao-g1s (人教版一年级上册)
     */
    private String textbookUri;
}