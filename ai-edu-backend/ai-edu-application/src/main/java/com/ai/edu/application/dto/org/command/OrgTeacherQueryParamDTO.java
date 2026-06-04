package com.ai.edu.application.dto.org.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 教职工查询参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgTeacherQueryParamDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 学校ID
     */
    private Long schoolId;

    /**
     * 部门ID
     */
    private Long departmentId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 页码（默认1）
     */
    private Integer pageNum;

    /**
     * 每页数量（默认10）
     */
    private Integer pageSize;
}