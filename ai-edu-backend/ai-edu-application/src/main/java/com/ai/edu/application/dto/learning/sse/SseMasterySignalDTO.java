package com.ai.edu.application.dto.learning.sse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * SSE {@code meta.masterySignals} 段（前端 camelCase，{kpLabel, signal}）。
 *
 * <p>区别于领域 {@code MasterySignalItem}（序列化为 snake_case {@code kp_label}），
 * 前端"知识点确认"阶段按 camelCase {@code kpLabel} 消费，故单独建 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseMasterySignalDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 知识点 label（如"二元一次方程组"） */
    private String kpLabel;

    /** 掌握度信号（mastered / practicing / struggling） */
    private String signal;
}
