package com.ai.edu.domain.organization.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 学年值对象
 * 格式: 2024-2025
 */
@Getter
@EqualsAndHashCode
public class SchoolYear implements ValueObject {

    private final String value;

    private SchoolYear(String value) {
        if (!isValidFormat(value)) {
            throw new IllegalArgumentException("Invalid school year format: " + value);
        }
        this.value = value;
    }

    public static SchoolYear of(String value) {
        return new SchoolYear(value);
    }

    public static SchoolYear current() {
        return of(java.time.LocalDate.now());
    }

    public static SchoolYear of(java.time.LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();
        // 学年从9月开始，如2024年9月-2025年8月为2024-2025学年
        if (month >= 9) {
            return new SchoolYear(year + "-" + (year + 1));
        } else {
            return new SchoolYear((year - 1) + "-" + year);
        }
    }

    public int getStartYear() {
        return Integer.parseInt(value.split("-")[0]);
    }

    public int getEndYear() {
        return Integer.parseInt(value.split("-")[1]);
    }

    public SchoolYear previous() {
        int startYear = getStartYear() - 1;
        return new SchoolYear(startYear + "-" + (startYear + 1));
    }

    public SchoolYear next() {
        int startYear = getStartYear() + 1;
        return new SchoolYear(startYear + "-" + (startYear + 1));
    }

    private boolean isValidFormat(String value) {
        if (value == null) return false;
        return value.matches("\\d{4}-\\d{4}");
    }

    @Override
    public String toString() {
        return value;
    }
}