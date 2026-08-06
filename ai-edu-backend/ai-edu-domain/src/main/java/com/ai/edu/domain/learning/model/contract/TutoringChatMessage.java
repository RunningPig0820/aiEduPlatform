package com.ai.edu.domain.learning.model.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 答疑对话消息（Redis 热存 + decide/generate 上下文 history 项，Java↔Python 契约 snake_case）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutoringChatMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 角色：user / ai */
    private String role;

    /** 消息内容 */
    private String content;

    /** 消息时间（Python 可忽略） */
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public static TutoringChatMessage user(String content) {
        return TutoringChatMessage.builder().role("user").content(content).createdAt(LocalDateTime.now()).build();
    }

    public static TutoringChatMessage ai(String content) {
        return TutoringChatMessage.builder().role("ai").content(content).createdAt(LocalDateTime.now()).build();
    }
}
