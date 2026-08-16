package com.ai.edu.domain.learning.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 题型标识值对象——归一化后的题型名（topic_key）。
 *
 * <p>学生题型掌握度以该 key 为稳定标识幂等落库（t_student_topic_mastery.topic_key），
 * 归一化由 {@link com.ai.edu.domain.learning.service.TopicKeyNormalizer} 完成。
 * 题型掌握度主体翻转后，知识点覆盖度由「题型掌握度 × 题型→知识点 ratio」派生。
 */
@Getter
@EqualsAndHashCode
public final class TopicKey implements ValueObject {

    private final String value;

    private TopicKey(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TopicKey must not be blank");
        }
        this.value = value;
    }

    public static TopicKey of(String value) {
        return new TopicKey(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
