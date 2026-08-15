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
 * 学生知识点掌握度单项（图谱叠加：按 kp_key URI 匹配节点渲染）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasteryItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 知识点 key（TextbookKP URI） */
    private String kpKey;

    /** 知识点名（冗余展示） */
    private String kpLabel;

    /** 掌握度分值 0-100 */
    private Integer masteryLevel;

    /** 解析状态：RESOLVED（确定）/ PENDING（疑似待确认） */
    private String status;

    /** 解析置信度 0-100（从派生观测关联） */
    private Integer confidence;

    /** 学段 primary/middle/high（从 kpKey 反查归属教材；无归属为 null） */
    private String stage;

    /** 归属章节名（无归属为 null） */
    private String chapterLabel;

    /** 归属小节名（无归属为 null） */
    private String sectionLabel;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
