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
 * RAG 助手 ask 请求（Java → Python 内部契约，snake_case 序列化）。
 *
 * <p>{@code history} 由 Java 网关组装（最近 N 轮，含 clarify 轮），{@code trace_id} 由 Java 生成
 * 传 Python（贯穿日志并在 done 回显）。前端 → Java 网关为 camelCase（见 {@code RagAskCommand}），
 * 桥内转换成本对象后序列化为 snake_case 调 Python。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagAskRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String question;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("current_project")
    private String currentProject;

    /** 最近 N 轮历史（含 clarify 轮），Java 组装 */
    private List<RagHistoryItem> history;

    @JsonProperty("trace_id")
    private String traceId;

    @JsonProperty("top_k")
    private Integer topK;
}
