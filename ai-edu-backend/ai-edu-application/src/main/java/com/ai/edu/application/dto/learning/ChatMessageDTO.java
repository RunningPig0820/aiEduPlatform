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
 * 对话消息 DTO（Redis 消息列表项，断点恢复返回）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 角色：user / ai */
    private String role;

    /** 消息内容 */
    private String content;

    /** 图片消息的 COS URL（图片题断点恢复展示用，文字消息为 null） */
    private String imageUrl;

    /** AI 推理过程文本（AI 消息含思考时返回，供"思考过程"折叠面板；无思考/学生消息为 null） */
    private String thinking;

    /** 消息时间 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
