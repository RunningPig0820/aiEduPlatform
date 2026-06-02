package com.ai.edu.application.dto.org.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 创建学校请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSchoolCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 学校名称
     */
    @NotBlank(message = "学校名称不能为空")
    @Size(max = 100, message = "学校名称最长100字符")
    private String name;

    /**
     * 学校图标URL
     */
    private String iconUrl;

    /**
     * 学校类型: KINDERGARTEN, PRIMARY, JUNIOR, SENIOR, COMPREHENSIVE
     */
    @NotBlank(message = "学校类型不能为空")
    private String type;

    /**
     * 包含学段: KINDERGARTEN, PRIMARY, JUNIOR, SENIOR
     */
    private List<String> stages;
}