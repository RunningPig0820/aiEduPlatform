package com.ai.edu.application.dto.kg;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 同步历史查询请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncRecordQueryRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "教材版本")
    private String edition;

    @ApiModelProperty(value = "学科")
    private String subject;

    @ApiModelProperty(value = "学段")
    private String stage;

    @ApiModelProperty(value = "年级")
    private String grade;

    @ApiModelProperty(value = "页码", example = "1")
    private Integer page;

    @ApiModelProperty(value = "每页数量", example = "10")
    private Integer size;
}