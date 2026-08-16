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
 * 学生题型掌握度单项（掌握度主体翻转：题型粒度）。
 *
 * <p>题型直接观测：topic_key（归一化题型标识）为主键，masteryLevel 四档 0/25/50/75，
 * status=RESOLVED（确定）/ PENDING（疑似待确认）。知识点视图见 {@code KpCoverageItemDTO}。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasteryItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 归一化题型标识（幂等主键） */
    private String topicKey;

    /** 题型展示名 */
    private String topicLabel;

    /** 掌握度四档 0/25/50/75 */
    private Integer masteryLevel;

    /** 解析状态：RESOLVED（确定）/ PENDING（疑似待确认） */
    private String status;

    /** 解析置信度 0-100（从派生观测关联） */
    private Integer confidence;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
