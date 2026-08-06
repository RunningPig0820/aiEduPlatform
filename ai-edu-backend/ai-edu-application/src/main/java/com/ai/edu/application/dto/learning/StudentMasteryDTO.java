package com.ai.edu.application.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 学生知识点掌握度响应（api.md 接口 7，图谱叠加数据源）。
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

    /** 掌握度列表（按 kp_key URI 叠加到知识图谱节点） */
    private List<MasteryItemDTO> items;
}
