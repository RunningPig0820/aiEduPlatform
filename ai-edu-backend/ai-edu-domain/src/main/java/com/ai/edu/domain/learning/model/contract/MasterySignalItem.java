package com.ai.edu.domain.learning.model.contract;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 掌握度信号项（mastery_signals 单项：{topic_label|kp_label, signal}，Java↔Python 内部契约 snake_case）。
 *
 * <p>掌握度主体翻转后 label 语义为<b>题型</b>（如「鸡兔同笼」），归一化后作题型掌握度主键。
 * 字段兼容 Python 新契约 {@code topic_label} 与旧契约 {@code kp_label}（过渡期，见 design Open Q0）。
 * signal 为 Python 原始小写字符串（mastered/practicing/struggling），
 * 经 {@link com.ai.edu.domain.learning.model.valueobject.MasterySignal#fromCode} 容错转换。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterySignalItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 题型 label（Python label 接地：优先复用快照候选） */
    @JsonProperty("topic_label")
    @JsonAlias("kp_label")
    private String kpLabel;

    /** 信号（mastered / practicing / struggling，小写，应用层容错转换） */
    private String signal;
}
