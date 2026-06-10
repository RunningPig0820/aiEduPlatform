package com.ai.edu.application.dto.org;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 学段配置 DTO
 * 包含学段编码、年制及可用的年级列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageConfigDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 学段编码 */
    private String stageCode;

    /** 学段中文名 */
    private String stageName;

    /** 年制编码 */
    private String stageYearCode;

    /** 年制中文名 */
    private String stageYearName;

    /** 年制年数 */
    private Integer yearCount;

    /** 起始年级（GradeLevel 1-12） */
    private Integer startGrade;

    /** 结束年级（GradeLevel 1-12） */
    private Integer endGrade;

    /** 该学段下的可用年级列表 */
    private List<GradeOptionDTO> grades;
}
