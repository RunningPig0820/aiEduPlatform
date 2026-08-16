package com.ai.edu.application.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 学生知识点派生覆盖度响应（掌握度主体翻转：知识点总览知识地图着色数据源）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpCoverageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 学生 ID */
    private Long studentId;

    /** 知识点覆盖度列表（按 kpUri 叠加到知识图谱节点） */
    private List<KpCoverageItemDTO> items;
}
