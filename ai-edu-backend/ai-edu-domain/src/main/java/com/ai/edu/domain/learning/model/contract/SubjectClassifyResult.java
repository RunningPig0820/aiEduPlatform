package com.ai.edu.domain.learning.model.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 学科分类结果（Python → Java）。
 *
 * <p>subject 闭集：math / physics / chemistry / biology / other；
 * 识别失败/异常 → 空 subject（Java 编排层按 math 放行，宁可漏拦不误拦）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectClassifyResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 识别学科（闭集 math/physics/chemistry/biology/other；失败为空） */
    private String subject;

    /** 学科是否为 math（放行）。 */
    public boolean isMath() {
        return "math".equals(subject);
    }

    /** 结果是否为空（识别失败 / 未返回 subject）——编排层按 math 放行。 */
    public boolean isEmpty() {
        return subject == null || subject.isBlank();
    }
}
