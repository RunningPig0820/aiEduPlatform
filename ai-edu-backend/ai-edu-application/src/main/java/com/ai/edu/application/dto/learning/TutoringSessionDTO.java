package com.ai.edu.application.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 答疑会话响应 DTO（断点恢复/会话列表）。
 *
 * <p>无 questionContent——后端不记录题目内容，前端从 recent_messages 推断当前题目。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutoringSessionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 会话ID */
    private Long sessionId;

    /** 会话状态：ACTIVE / ARCHIVED / TERMINATED */
    private String status;

    /** 学科（本期恒为 math） */
    private String subject;

    /** 题型（可空） */
    private String questionType;

    /** 轮次计数 */
    private Integer roundCount;

    /** 要答案次数 */
    private Integer answerRequestCount;

    /** 最近消息（Redis 热存，供前端续聊/推断当前题目） */
    private List<ChatMessageDTO> recentMessages;

    /** 收尾总结（结构化：知识点/薄弱点，可空） */
    private SummaryDTO summary;

    /** COS 对话归档 objectKey（可空） */
    private String transcriptUrl;
}
