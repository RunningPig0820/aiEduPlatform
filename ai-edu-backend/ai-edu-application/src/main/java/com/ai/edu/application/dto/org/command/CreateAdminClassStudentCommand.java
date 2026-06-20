package com.ai.edu.application.dto.org.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 添加行政班学生命令
 *
 * 提交学生基本信息（姓名、手机号、身份证号、学号）和家长列表。
 * 组织域负责编排跨域调用，最终创建 StudentClass 关联关系。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdminClassStudentCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 学生姓名
     */
    @NotBlank(message = "学生姓名不能为空")
    private String name;

    /**
     * 学生手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 身份证号（18位，明文传入，加密存储）
     */
    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号格式不正确（应为18位，末位可为X）")
    private String idCard;

    /**
     * 学号（可选）
     */
    private String studentNo;

    /**
     * 家长列表（可选，支持多家长绑定）
     */
    @Valid
    private List<ParentCommand> parents;
}
