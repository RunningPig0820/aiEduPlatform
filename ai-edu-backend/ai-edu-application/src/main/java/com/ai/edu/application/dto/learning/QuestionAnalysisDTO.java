package com.ai.edu.application.dto.learning;

import com.ai.edu.domain.learning.model.valueobject.KpResolution;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 单题分析响应（POST /api/kp/analyze-question）——题目文本 → 题型 → 关联知识点清单。
 *
 * <p>status=RESOLVED（knowledgePoints 非空）/ PENDING（knowledgePoints 空 + candidates，不报错）。
 * 纯分析不写观测；学生确认走 POST /api/kp/vote。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAnalysisDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 识别出的题型名；PENDING 时为首个候选题型名（可为 null） */
    private String topicLabel;

    /** RESOLVED / PENDING */
    private String status;

    /** 置信度 0-100；PENDING 为 0 */
    private Integer confidence;

    /** 关联知识点清单（题型库命中全分布 / 未命中单点 ratio=1 / PENDING 空） */
    private List<QuestionAnalysisKpDTO> knowledgePoints;

    /** PENDING 时的澄清候选（学科概念名，不暴露 kp_uri）；RESOLVED 为空 */
    private List<String> candidates;

    public static QuestionAnalysisDTO resolved(String topicLabel, int confidence,
                                               List<QuestionAnalysisKpDTO> knowledgePoints) {
        return QuestionAnalysisDTO.builder()
                .topicLabel(topicLabel)
                .status(KpResolution.STATUS_RESOLVED)
                .confidence(confidence)
                .knowledgePoints(knowledgePoints)
                .candidates(List.of())
                .build();
    }

    public static QuestionAnalysisDTO pending(String topicLabel, List<String> candidates) {
        return QuestionAnalysisDTO.builder()
                .topicLabel(topicLabel)
                .status(KpResolution.STATUS_PENDING)
                .confidence(0)
                .knowledgePoints(List.of())
                .candidates(candidates == null ? List.of() : candidates)
                .build();
    }
}
