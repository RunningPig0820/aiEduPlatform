package com.ai.edu.domain.homework.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 作业状态值对象
 */
@Getter
@EqualsAndHashCode
public class HomeworkStatus implements ValueObject {

    private final String value;

    private HomeworkStatus(String value) {
        this.value = value;
    }

    public static HomeworkStatus pending() {
        return new HomeworkStatus("PENDING");
    }

    public static HomeworkStatus processing() {
        return new HomeworkStatus("PROCESSING");
    }

    public static HomeworkStatus completed() {
        return new HomeworkStatus("COMPLETED");
    }

    public static HomeworkStatus failed() {
        return new HomeworkStatus("FAILED");
    }

    public boolean isPending() {
        return "PENDING".equals(value);
    }

    public boolean isProcessing() {
        return "PROCESSING".equals(value);
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(value);
    }

    public boolean isFailed() {
        return "FAILED".equals(value);
    }
}