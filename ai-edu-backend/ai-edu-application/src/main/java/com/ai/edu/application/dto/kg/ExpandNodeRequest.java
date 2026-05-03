package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 节点展开请求参数
 * 用于展开任意节点的直接邻居（节点 + 边）
 *
 * URI 示例: http://edukg.org/knowledge/3.1/kp/math#...
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpandNodeRequest {
    /** 节点 URI */
    private String nodeUri;

    /** 返回数量上限，默认 20 */
    private int limit;
}
