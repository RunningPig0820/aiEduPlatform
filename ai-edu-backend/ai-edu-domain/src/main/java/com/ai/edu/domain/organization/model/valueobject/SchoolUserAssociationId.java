package com.ai.edu.domain.organization.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

/**
 * 学校用户关联ID值对象
 */
@Getter
@EqualsAndHashCode
public class SchoolUserAssociationId implements ValueObject, Serializable {

    private final Long value;

    private SchoolUserAssociationId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("SchoolUserAssociationId must be positive");
        }
        this.value = value;
    }

    public static SchoolUserAssociationId of(Long value) {
        return new SchoolUserAssociationId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}