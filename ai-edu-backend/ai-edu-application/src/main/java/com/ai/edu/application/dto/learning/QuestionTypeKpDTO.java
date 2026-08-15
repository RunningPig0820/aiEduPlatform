package com.ai.edu.application.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 题型关联知识点条目（GET /api/kp/question-types/{id}/knowledge-points）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionTypeKpDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 知识点 URI */
    private String kpUri;

    /** 知识点名（kg 镜像反查） */
    private String kpLabel;

    /** 该 kp 覆盖年级段 */
    private String gradeRange;

    /** 该 kp 占比 */
    private Double ratio;

    /** 该分布桶命中次数 */
    private Integer hitCount;
}
