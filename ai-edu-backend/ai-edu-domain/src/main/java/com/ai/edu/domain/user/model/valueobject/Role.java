package com.ai.edu.domain.user.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 角色值对象
 */
@Getter
@EqualsAndHashCode
public class Role implements ValueObject {

    private final String value;

    private Role(String value) {
        this.value = value;
    }

    public static Role student() {
        return new Role("STUDENT");
    }

    public static Role teacher() {
        return new Role("TEACHER");
    }

    public static Role parent() {
        return new Role("PARENT");
    }

    public static Role admin() {
        return new Role("ADMIN");
    }

    public static Role of(String value) {
        return new Role(value);
    }

    public boolean isStudent() {
        return "STUDENT".equals(value);
    }

    public boolean isTeacher() {
        return "TEACHER".equals(value);
    }

    public boolean isParent() {
        return "PARENT".equals(value);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(value);
    }
}