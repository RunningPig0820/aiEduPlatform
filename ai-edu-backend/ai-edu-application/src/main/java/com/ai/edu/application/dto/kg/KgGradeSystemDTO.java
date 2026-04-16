package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 年级知识体系 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KgGradeSystemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String grade;
    private String groupBy;
    private List<GroupDTO> groups;
    private int totalKnowledgePoints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupDTO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String key;
        private String label;
        private List<ChapterNode> chapters;
        private int knowledgePointCount;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ChapterNode implements Serializable {
            @Serial
            private static final long serialVersionUID = 1L;

            private String uri;
            private String label;
            private String topic;
            private Integer orderIndex;
            private List<SectionNode> sections;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SectionNode implements Serializable {
            @Serial
            private static final long serialVersionUID = 1L;

            private String uri;
            private String label;
            private List<KgKnowledgePointDTO> knowledgePoints;
        }
    }
}
