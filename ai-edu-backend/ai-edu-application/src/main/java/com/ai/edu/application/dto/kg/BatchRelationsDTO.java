package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 批量概念关联请求/响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchRelationsDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<RelationEntry> relations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationEntry implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String uri;
        private List<String> relatedUris;
    }
}
