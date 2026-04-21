package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Grade 请求参数（用于 POST 接口）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeRequest {
    /**
     * 年级（可能包含中文）
     */
    private String grade;
}