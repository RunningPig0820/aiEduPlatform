package com.ai.edu.domain.learning.model.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * RAG 助手会话历史项（Java 网关组装，最近 N 轮，含 clarify 轮，Python 只消费）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagHistoryItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 该轮学生问题 */
    private String question;

    /** 该轮回答（boundary/clarify 轮可空） */
    private String answer;

    /** 该轮成功锚定的模块 id */
    private String anchor;
}
