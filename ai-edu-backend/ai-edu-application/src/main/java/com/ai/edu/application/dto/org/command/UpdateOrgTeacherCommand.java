package com.ai.edu.application.dto.org.command;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新教职工命令
 * 组织域只支持修改所属部门
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrgTeacherCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("orgTeacherId")
    @NotNull(message = "orgTeacherId 不能为空")
    private Long orgTeacherId;


    @ApiModelProperty("新的所属行政部门ID")
    @NotNull(message = "所属部门不能为空")
    private Long departmentId;
}