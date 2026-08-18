package com.ai.edu.domain.learning.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 题目聚合（tasks 3.1）——「一道题的一次完整作答」的跨轮状态容器。
 *
 * <p>一次会话内可能多道题（换题判定在 Python decide，Java 认 {@code SWITCH} 事件 / 新图片 isNewQuestion）。
 * 每道题 = 从换题到下次换题之间的轮次集合：{@link TutoringSession#onRoundSignal} 逐轮累计，
 * {@link TutoringSession#settleAttempt()} 换题/结束结算返回一条聚合——3.4 落题目表时消费
 * （content + topicLabel 过聚集 canonical + score 由 3.3 信号映射算）。
 *
 * <p>随 {@link TutoringSession} 经 Jackson 序列化存 Redis（跨轮保留）；不落 DB PO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAttempt implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 该题累计的轮信号（decide 逐轮，3.3 合并算生效分值） */
    private List<RoundSignal> rounds = new ArrayList<>();

    /** 当前题题型名（首轮识别，3.4 过聚集 canonical） */
    private String topicLabel;

    /** 题目文本（换题后首条 user 消息，非最后一条用户消息，3.2） */
    private String content;

    /** 已累计轮数（= rounds.size()） */
    private int roundCount;
}
