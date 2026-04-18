package com.ai.edu.domain.edukg.model.entity.relation;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 教材-章节关联实体
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgTextbookChapter {

    private Long id;

    private String textbookUri;

    private String chapterUri;

    private Integer orderIndex;

    private Long createdBy = 0L;

    private Long modifiedBy = 0L;

    private Boolean deleted = false;

    public static KgTextbookChapter create(String textbookUri, String chapterUri, Integer orderIndex) {
        KgTextbookChapter relation = new KgTextbookChapter();
        relation.textbookUri = textbookUri;
        relation.chapterUri = chapterUri;
        relation.orderIndex = orderIndex;
        return relation;
    }
}
