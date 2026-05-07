package com.ai.edu.domain.organization.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 学校性质类型值对象
 * 表示学校的机构性质：公立、私立、培训机构
 */
@Getter
@EqualsAndHashCode
public class SchoolInstitutionalType implements ValueObject {

    private final String value;

    private SchoolInstitutionalType(String value) {
        this.value = value;
    }

    /**
     * 公立学校
     */
    public static SchoolInstitutionalType publicSchool() {
        return new SchoolInstitutionalType("PUBLIC");
    }

    /**
     * 私立学校
     */
    public static SchoolInstitutionalType privateSchool() {
        return new SchoolInstitutionalType("PRIVATE");
    }

    /**
     * 培训机构
     */
    public static SchoolInstitutionalType trainingInstitute() {
        return new SchoolInstitutionalType("TRAINING_INSTITUTE");
    }

    public static SchoolInstitutionalType of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SchoolInstitutionalType value cannot be null or blank");
        }
        return new SchoolInstitutionalType(value);
    }

    public boolean isPublic() {
        return "PUBLIC".equals(value);
    }

    public boolean isPrivate() {
        return "PRIVATE".equals(value);
    }

    public boolean isTrainingInstitute() {
        return "TRAINING_INSTITUTE".equals(value);
    }

    public String getDescription() {
        return switch (value) {
            case "PUBLIC" -> "公立学校";
            case "PRIVATE" -> "私立学校";
            case "TRAINING_INSTITUTE" -> "培训机构";
            default -> value;
        };
    }

    @Override
    public String toString() {
        return value;
    }
}