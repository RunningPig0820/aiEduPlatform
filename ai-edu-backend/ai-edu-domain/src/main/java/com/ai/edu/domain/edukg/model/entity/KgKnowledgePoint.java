package com.ai.edu.domain.edukg.model.entity;

import com.ai.edu.domain.edukg.model.valueobject.KgDifficulty;
import com.ai.edu.domain.edukg.model.valueobject.KgImportance;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 知识图谱-知识点实体
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgKnowledgePoint {

    private Long id;

    private String uri;

    private String label;

    private String difficulty;

    private String importance;

    private String cognitiveLevel;

    private String status;

    private String mergedToUri;

    private Long createdBy = 0L;

    private Long modifiedBy = 0L;

    private Boolean deleted = false;

    public static KgKnowledgePoint create(String uri, String label) {
        KgKnowledgePoint kp = new KgKnowledgePoint();
        kp.uri = uri;
        kp.label = label;
        kp.status = "active";
        return kp;
    }

    public void updateAttributes(String difficulty, String importance, String cognitiveLevel) {
        this.difficulty = difficulty;
        this.importance = importance;
        this.cognitiveLevel = cognitiveLevel;
    }

    public boolean isHighImport() {
        return KgImportance.HIGH.getValue().equals(this.importance);
    }
}
