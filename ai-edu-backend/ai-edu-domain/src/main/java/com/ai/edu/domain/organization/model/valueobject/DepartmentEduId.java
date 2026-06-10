package com.ai.edu.domain.organization.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 教育部门扩展属性 ID 值对象
 */
@Getter
@EqualsAndHashCode
public class DepartmentEduId implements ValueObject {

    private final Long value;

    private DepartmentEduId(Long value) {
        if (value == null) {
            throw new IllegalArgumentException("DepartmentEduId value cannot be null");
        }
        this.value = value;
    }

    public static DepartmentEduId of(Long value) {
        return new DepartmentEduId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
