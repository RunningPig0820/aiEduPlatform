package com.ai.edu.domain.edukg.model.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 知识图谱-小节实体
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgSection {

    private Long id;

    private String uri;

    private String label;

    private String status;

    private String mergedToUri;

    private Long createdBy = 0L;

    private Long modifiedBy = 0L;

    private Boolean deleted = false;

    public static KgSection create(String uri, String label) {
        KgSection section = new KgSection();
        section.uri = uri;
        section.label = label;
        section.status = "active";
        return section;
    }
}
