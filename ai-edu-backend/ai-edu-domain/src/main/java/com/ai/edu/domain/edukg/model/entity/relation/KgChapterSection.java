package com.ai.edu.domain.edukg.model.entity.relation;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 章节-小节关联实体
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgChapterSection {

    private Long id;

    private String chapterUri;

    private String sectionUri;

    private Integer orderIndex;

    private Long createdBy = 0L;

    private Long modifiedBy = 0L;

    private Boolean deleted = false;

    public static KgChapterSection create(String chapterUri, String sectionUri, Integer orderIndex) {
        KgChapterSection relation = new KgChapterSection();
        relation.chapterUri = chapterUri;
        relation.sectionUri = sectionUri;
        relation.orderIndex = orderIndex;
        return relation;
    }
}
