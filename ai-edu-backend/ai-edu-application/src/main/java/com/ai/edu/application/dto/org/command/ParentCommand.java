package com.ai.edu.application.dto.org.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 家长信息命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 家长姓名
     */
    @NotBlank(message = "家长姓名不能为空")
    private String name;

    /**
     * 家长手机号
     */
    @NotBlank(message = "家长手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "家长手机号格式不正确")
    private String phone;

    /**
     * 与学生关系类型（父亲/母亲/监护人等）
     */
    @NotBlank(message = "关系类型不能为空")
    private String relationship;
}
