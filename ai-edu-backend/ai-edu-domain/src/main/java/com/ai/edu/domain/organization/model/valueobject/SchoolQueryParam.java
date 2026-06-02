package com.ai.edu.domain.organization.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 学校查询参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolQueryParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 学校ID
     */
    private Long id;

    /**
     * 学校名称（模糊查询）
     */
    private String name;

    /**
     * 学校类型
     */
    private String type;

    /**
     * 页码
     */
    private Integer pageNum;

    /**
     * 每页数量
     */
    private Integer pageSize;
}