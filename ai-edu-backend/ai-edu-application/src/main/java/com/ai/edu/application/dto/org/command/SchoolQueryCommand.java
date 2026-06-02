package com.ai.edu.application.dto.org.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 学校查询请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolQueryCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 页码（默认第1页）
     */
    private Integer pageNum = 1;

    /**
     * 每页数量（默认10条）
     */
    private Integer pageSize = 10;

    /**
     * 学校ID
     */
    private Long id;

    /**
     * 学校名称（模糊查询）
     */
    private String name;

    /**
     * 学校类型: KINDERGARTEN, PRIMARY, JUNIOR, SENIOR, COMPREHENSIVE
     */
    private String type;
}