package com.ai.edu.domain.learning.model.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Python generate 请求上下文（Java→Python 内部契约，snake_case）。
 *
 * <p>{@code action_type} 为<b>护栏已放行</b>的 type（可能异于 decide 原始输出，如 reveal 被拦→approach），
 * Python 据此约束生成正文与 type 一致。{@code action_meta} 携带原动作元数据（eval/summary 等）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 完整对话历史 */
    private List<TutoringChatMessage> history;

    /** 学科提示（本期恒 math） */
    @JsonProperty("subject_hint")
    private String subjectHint;

    /** 已放行动作类型（hint/approach/reveal/concept/switch/end） */
    @JsonProperty("action_type")
    private String actionType;

    /** 原动作元数据（Python 可忽略部分字段） */
    @JsonProperty("action_meta")
    private ActionMeta actionMeta;
}
