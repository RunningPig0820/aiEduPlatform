package com.ai.edu.application.dto.kg;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 同步请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "教材")
    private String edition;
    @ApiModelProperty(value = "学科")
    private String subject;
    @ApiModelProperty(value = "学段")
    private String stage;
    @ApiModelProperty(value = "年级")
    private String grade;

}
