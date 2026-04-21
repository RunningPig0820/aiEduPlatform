package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 年级知识体系请求参数（用于 POST 接口）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeSystemRequest {
    /**
     * 年级（可能包含中文）
     */
    private String grade;

    /**
     * 分组方式：subject / textbook
     */
    private String groupBy;
}