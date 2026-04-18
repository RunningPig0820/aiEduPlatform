package com.ai.edu.domain.edukg.model.entity;

import com.ai.edu.domain.edukg.model.valueobject.KgNodeStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 知识图谱-教材实体
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgTextbook {

    private Long id;

    private String uri;

    private String label;

    private String grade;

    private String stage;

    private String edition;

    private Integer orderIndex = 0;

    private String subject;

    private String status;

    private String mergedToUri;

    private Long createdBy = 0L;

    private Long modifiedBy = 0L;

    private Boolean deleted = false;

    public static KgTextbook create(String uri, String label, String grade, String stage, String edition, String subject) {
        KgTextbook textbook = new KgTextbook();
        textbook.uri = uri;
        textbook.label = label;
        textbook.grade = grade;
        textbook.stage = stage;
        textbook.edition = edition;
        textbook.subject = subject;
        textbook.orderIndex = 0;
        textbook.status = "active";
        return textbook;
    }

    public void markDeleted(String mergedToUri) {
        this.status = KgNodeStatus.MERGED.getValue();
        this.mergedToUri = mergedToUri;
    }

    public void updateFrom(KgTextbook other) {
        this.label = other.label;
        this.grade = other.grade;
        this.stage = other.stage;
        this.edition = other.edition;
        this.subject = other.subject;
        this.orderIndex = other.orderIndex;
    }

    public boolean isMerged() {
        return KgNodeStatus.MERGED.getValue().equals(this.status);
    }
}
