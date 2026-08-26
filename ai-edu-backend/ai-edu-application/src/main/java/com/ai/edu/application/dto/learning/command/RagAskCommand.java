package com.ai.edu.application.dto.learning.command;

import com.ai.edu.domain.learning.model.contract.RagHistoryItem;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * RAG 项目介绍助手发起问答命令（前端 → Java 网关，camelCase）。
 *
 * <p>角色由 session 判定（STUDENT 才放行），body 不传身份；{@code traceId} 由 Java 网关生成。
 * {@code history} 由**前端传**（最近 3 轮 {question, answer, anchor}，追问展开用——省略主语的
 * "能说的详细一点吗" 靠 history 还原；刷新后前端消息列表清空则 history 为空，等价新会话）。
 * {@code stream=false} 走非流式（stages 摘要），默认 SSE 流式。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagAskCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "问题不能为空")
    private String question;

    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /** 页面锚定模块（模块 id 闭集：ai-tutoring/knowledge-graph/question-analysis/rag-system），缺省 rag-system */
    private String currentProject;

    /** RRF 精排回传块数（默认 3） */
    private Integer topK;

    /** true=SSE 流式；false/缺省=非流式（stages 摘要） */
    private Boolean stream;

    /** 最近 3 轮 {question, answer, anchor}（前端传，追问展开用；空=无上下文） */
    private List<RagHistoryItem> history;
}
