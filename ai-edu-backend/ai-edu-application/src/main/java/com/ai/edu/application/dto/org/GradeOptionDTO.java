package com.ai.edu.application.dto.org;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 年级选项 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeOptionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 年级编码（GradeLevel 1-12） */
    private Integer gradeCode;

    /** 年级显示名 */
    private String gradeName;
}
