package com.ai.edu.application.dto.learning;

import com.ai.edu.domain.learning.model.contract.TutoringChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 答疑会话 transcript 响应 DTO（后端代理读 COS 透传，前端零 COS 直连）。
 *
 * <p>messages 结构与 COS transcript JSON 一致（含 meta：type/denied/decide_reason/round/question_kps/eval/status），
 * 前端 {@code toMessage} 逐条复原；对象缺失/未归档时为空数组。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutoringTranscriptDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 完整对话消息列表（含 meta；COS 对象缺失/未归档 → 空） */
    private List<TutoringChatMessage> messages;
}
