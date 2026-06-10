package com.ai.edu.application.dto.org.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建行政班节点命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdminClassNodeCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 节点名称 */
    @NotBlank(message = "节点名称不能为空")
    private String name;

    /** 所属学校ID */
    @NotNull(message = "学校ID不能为空")
    private Long schoolId;

    /** 父节点ID（不填则为根节点） */
    private Long parentId;

    /** 节点类型：3-学段, 4-年级, 5-班级 */
    @NotNull(message = "节点类型不能为空")
    private Integer deptType;

    /** 学段编码 */
    @NotBlank(message = "学段编码不能为空")
    private String stageCode;

    /** 年制编码 */
    @NotBlank(message = "年制编码不能为空")
    private String stageYearCode;

    /** 年级编码（年级和班级节点必填） */
    private String gradeCode;

    /** 入学年份（年级和班级节点必填） */
    private String enrollmentYear;

    /** 排序序号 */
    private Integer sortOrder;

    /** 描述 */
    private String description;
}
