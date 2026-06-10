package com.ai.edu.application.dto.org;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 行政班节点响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminClassNodeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 部门ID */
    private Long deptId;

    /** 学校ID */
    private Long schoolId;

    /** 节点名称 */
    private String name;

    /** 父节点ID */
    private Long parentId;

    /** 部门路径 */
    private String departmentPath;

    /** 部门类型 */
    private String departmentType;

    /** 节点类型：3-学段, 4-年级, 5-班级 */
    private Integer deptType;

    /** 学段编码 */
    private String stageCode;

    /** 年制编码 */
    private String stageYearCode;

    /** 年级编码 */
    private String gradeCode;

    /** 入学年份 */
    private String enrollmentYear;

    /** 排序序号 */
    private Integer sortOrder;

    /** 描述 */
    private String description;

    /** 子节点列表 */
    private List<AdminClassNodeDTO> children;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
