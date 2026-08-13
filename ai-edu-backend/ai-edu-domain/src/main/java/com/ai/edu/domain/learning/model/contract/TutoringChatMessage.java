package com.ai.edu.domain.learning.model.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

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

    /** AI 推理过程文本（generate thinking 分片拼接；学生消息/无思考轮为 null） */
    @JsonProperty("thinking")
    private String thinking;

    /** 消息时间（Python 可忽略） */
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    /** 工作流 meta：本消息动作类型（AI 消息生效类型，护栏降级后；用户消息为空） */
    private String type;

    /** 护栏拒绝时的原始请求类型（如 reveal；无拒绝为空） */
    private String denied;

    /** Python 决策自由文本（前端"为什么"hover 补充，可空） */
    @JsonProperty("decide_reason")
    private String decideReason;

    /** 当前轮次（本消息所属轮） */
    private Integer round;

    /** 题目涉及知识点（decide 读题分析，可空） */
    @JsonProperty("question_kps")
    private List<String> questionKps;

    /** 学生回答评估（EvalInfo，snake_case 内字段；可空） */
    private EvalInfo eval;

    /** 该轮会话状态（ACTIVE/ARCHIVED/TERMINATED，前端 ⑥ 归档点亮判定） */
    private String status;

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

    /** AI 回复含推理过程：thinking 为 generate thinking 分片拼接（可 null = 无思考轮）。 */
    public static TutoringChatMessage ai(String content, String thinking) {
        return TutoringChatMessage.builder().role("ai").content(content)
                .thinking(thinking).createdAt(LocalDateTime.now()).build();
    }
}
