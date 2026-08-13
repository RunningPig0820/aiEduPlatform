package com.ai.edu.application.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 答疑会话列表项 DTO（历史列表，updated_at 倒序）。
 *
 * <p>不含对话内容——内容由前端经 {@code transcriptUrl} 拉 COS transcript（见 api.md）。
 * 字段名沿用 {@code sessionId}（与 {@link TutoringSessionDTO} 一致）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutoringSessionListItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 会话ID */
    private Long sessionId;

    /** 会话标题（首条用户消息前 ~30 字；存量/兜底为空串或「图片题目」） */
    private String title;

    /** 会话状态：ACTIVE / ARCHIVED / TERMINATED */
    private String status;

    /** 学科（本期恒为 math） */
    private String subject;

    /** 题型（可空） */
    private String questionType;

    /** 轮次计数 */
    private Integer roundCount;

    /** 更新时间（列表倒序依据） */
    private LocalDateTime updatedAt;

    /** 归档时间（可空） */
    private LocalDateTime archivedAt;
}
