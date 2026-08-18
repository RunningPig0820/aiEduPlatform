package com.ai.edu.application.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 学生题型掌握度响应（掌握度主体翻转：题型粒度，api.md 接口 2，4.1 分页改造）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentMasteryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 学生 ID */
    private Long studentId;

    /** 题型掌握度列表（当前页） */
    private List<MasteryItemDTO> items;

    /** 总条数（筛选后） */
    private Integer total;

    /** 当前页码 */
    private Integer pageNum;

    /** 每页条数 */
    private Integer pageSize;
}
