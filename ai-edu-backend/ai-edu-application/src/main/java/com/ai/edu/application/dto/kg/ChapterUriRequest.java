package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 章节 URI 请求参数
 * 用于查询章节下的小节列表
 *
 * URI 示例: http://edukg.org/knowledge/3.1/chapter/math#renjiao-g1s-2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterUriRequest {
    /**
     * 章节 URI
     * 格式: http://edukg.org/knowledge/3.1/chapter/{subject}#{edition}-{grade}-{chapter}
     * 示例: http://edukg.org/knowledge/3.1/chapter/math#renjiao-g1s-2 (人教版一年级上册第2章)
     */
    private String chapterUri;
}