package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 章节树节点 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterTreeNode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String uri;
    private String label;
    private String topic;
    private Integer orderIndex;
    private List<SectionNode> sections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionNode implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String uri;
        private String label;
        private Integer orderIndex;
        private Integer knowledgePointCount;
    }
}
