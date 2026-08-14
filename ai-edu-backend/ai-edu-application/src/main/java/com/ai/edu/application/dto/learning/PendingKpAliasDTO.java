package com.ai.edu.application.dto.learning;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 挂起派生观测项（GET /api/kg/aliases/pending）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingKpAliasDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 观测 ID（确认用） */
    private Long id;

    /** 题型 label */
    private String topicLabel;

    /** 来源学生 */
    private Long studentId;

    /** 解析时年级 */
    private Integer studentGrade;

    /** 置信度 */
    private Integer confidence;

    /** PENDING / HUMAN_REVIEW */
    private String status;

    /** 若有候选归属 */
    private String kpUri;

    /** 首次记录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime firstSeenAt;
}
