package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 维度配置响应 DTO（学科/学段/教材下拉选项）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KgDimensionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 维度编码（学科/学段用 code，教材用 uri）
     */
    private String code;

    /**
     * 前端显示名称
     */
    private String label;

    /**
     * 排序序号
     */
    private Integer orderIndex;

    // 以下字段仅教材维度使用
    /**
     * 所属学科（教材维度）
     */
    private String subject;

    /**
     * 所属年级（教材维度）
     */
    private String grade;

    /**
     * 所属学段（教材维度）
     */
    private String phase;
}
