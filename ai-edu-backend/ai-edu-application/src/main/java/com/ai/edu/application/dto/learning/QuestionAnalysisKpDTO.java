package com.ai.edu.application.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 单题分析——关联知识点条目（POST /api/kp/analyze-question）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAnalysisKpDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 教材知识点 TextbookKP URI */
    private String kpUri;

    /** 知识点名（从 kg 镜像按 kpUri 反查） */
    private String kpLabel;

    /** 年级分布段（如 4-6 / 7），可为 null */
    private String gradeRange;

    /** 该知识点在题型中的占比（分布桶归一化和=1；单点解析为 1.0） */
    private Double ratio;
}
