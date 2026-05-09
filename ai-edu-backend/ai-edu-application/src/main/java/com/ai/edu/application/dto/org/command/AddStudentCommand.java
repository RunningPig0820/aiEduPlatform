package com.ai.edu.application.dto.org.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 添加学生到班级命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddStudentCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "学生ID不能为空")
    @Positive(message = "学生ID必须为正数")
    private Long studentId;

    private String studentNo;
}