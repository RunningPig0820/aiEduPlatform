package com.ai.edu.application.dto.org.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 部门查询命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentQueryCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 部门ID
     */
    private Long id;

    /**
     * 部门名称（模糊查询）
     */
    private String name;

    /**
     * 上级部门ID
     */
    private Long parentId;

    /**
     * 是否只查询根部门
     */
    private Boolean rootOnly;

    /**
     * 页码
     */
    private Integer pageNum;

    /**
     * 每页数量
     */
    private Integer pageSize;
}