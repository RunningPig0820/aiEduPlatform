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

    /** 消息内容（纯图片消息可为空） */
    private String content;

    /** 图片消息 URL（题目/示例图，COS 公开或签名 URL；非图片消息为 null） */
    @JsonProperty("image_url")
    private String imageUrl;

    /** 消息时间（Python 可忽略） */
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public static TutoringChatMessage user(String content) {
        return TutoringChatMessage.builder().role("user").content(content).createdAt(LocalDateTime.now()).build();
    }

    /** 图片消息：题目/示例图以 URL 进入对话（看图答疑用，换题信号在 decide 的 is_new_question）。 */
    public static TutoringChatMessage userWithImage(String content, String imageUrl) {
        // content 归一化空串：Python ChatTurn.content 是必填 str，null 会 422（纯图片消息无正文）
        return TutoringChatMessage.builder().role("user")
                .content(content == null ? "" : content)
                .imageUrl(imageUrl).createdAt(LocalDateTime.now()).build();
    }

    public static TutoringChatMessage ai(String content) {
        return TutoringChatMessage.builder().role("ai").content(content).createdAt(LocalDateTime.now()).build();
    }
}
