package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * URI 请求参数（用于 POST 接口）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UriRequest {
    /**
     * 节点 URI（可能包含中文，较长）
     */
    private String uri;
}