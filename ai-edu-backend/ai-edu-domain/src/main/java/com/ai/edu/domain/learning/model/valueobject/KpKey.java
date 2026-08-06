package com.ai.edu.domain.learning.model.valueobject;

import com.ai.edu.domain.shared.valueobject.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 知识点 key 值对象——包装知识图谱 TextbookKP 节点 URI。
 *
 * <p>学生掌握度以该 key 为稳定标识幂等落库（t_student_kp_mastery.kp_key = URI），
 * 图谱前端按 URI 叠加掌握度。label → URI 解析由 {@code TutoringKpResolver} 完成。
 */
@Getter
@EqualsAndHashCode
public final class KpKey implements ValueObject {

    private final String value;

    private KpKey(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("KpKey must not be blank");
        }
        this.value = value;
    }

    public static KpKey of(String value) {
        return new KpKey(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
