package com.ai.edu.application.dto.kg;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 年级教材 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeTextbookDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "年级")
    private String grade;

    @ApiModelProperty(value = "年级详情")
    private String label;

    @ApiModelProperty(value = "教材URI")
    private String textbookUri;
}