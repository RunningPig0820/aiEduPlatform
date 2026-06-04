package com.ai.edu.application.dto.org.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建教职工命令
 * 提交用户基本信息（姓名、手机号）和所属部门ID
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrgTeacherCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户姓名
     */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 所属行政部门ID
     */
    @NotNull(message = "所属部门不能为空")
    private Long departmentId;
}