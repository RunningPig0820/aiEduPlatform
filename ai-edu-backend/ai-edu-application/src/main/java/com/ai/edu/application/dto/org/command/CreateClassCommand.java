package com.ai.edu.application.dto.org.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 创建班级命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClassCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "班级名称不能为空")
    private String name;

    private String code;

    @NotNull(message = "学校ID不能为空")
    @Positive(message = "学校ID必须为正数")
    private Long schoolId;

    @NotNull(message = "年级不能为空")
    @Positive(message = "年级必须在1-12之间")
    private Integer grade;

    @NotBlank(message = "学年不能为空")
    private String schoolYear;

    private String classType;

    private String description;

    private Long headTeacherId;

    private String subject;
}