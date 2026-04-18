package com.ai.edu.domain.edukg.model.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 知识图谱-章节实体
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgChapter {

    private Long id;

    private String uri;

    private String label;

    private String topic;

    private String status;

    private String mergedToUri;

    private Long createdBy = 0L;

    private Long modifiedBy = 0L;

    private Boolean deleted = false;

    public static KgChapter create(String uri, String label) {
        KgChapter chapter = new KgChapter();
        chapter.uri = uri;
        chapter.label = label;
        chapter.status = "active";
        return chapter;
    }

    public void updateTopic(String topic) {
        this.topic = topic;
    }
}
