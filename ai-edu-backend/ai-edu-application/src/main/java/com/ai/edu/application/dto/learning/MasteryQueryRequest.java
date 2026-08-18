package com.ai.edu.application.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 掌握度分页查询请求（tasks 4.1 分页改造）。
 *
 * <p>默认：pageNum=1 / pageSize=20 / masteryStatus=all / sortBy=updatedAt / order=desc。
 * masteryStatus 分桶：all | consolidate(&lt;25 待巩固) | learning(25-50 练习中) | steady(50-75 偏稳) | mastered(≥75 已掌握)。
 * sortBy：updatedAt（默认，修改时间倒序）/ masteryLevel（掌握度）；order：desc / asc。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasteryQueryRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 页码（从 1 起，默认 1） */
    private Integer pageNum;

    /** 每页条数（默认 20，上限 100） */
    private Integer pageSize;

    /** 题型名模糊搜索（可选） */
    private String keyword;

    /** 掌握度分桶筛选：all | consolidate | learning | steady | mastered */
    private String masteryStatus;

    /** 排序字段：updatedAt（默认）| masteryLevel */
    private String sortBy;

    /** 排序方向：desc（默认）| asc */
    private String order;
}
