package com.ai.edu.application.dto.learning.command;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * RAG 项目介绍助手发起问答命令（前端 → Java 网关，camelCase）。
 *
 * <p>角色由 session 判定（STUDENT 才放行），body 不传身份；{@code history}/{@code traceId}
 * 由 Java 网关组装，前端不传。{@code stream=false} 走非流式（stages 摘要），默认 SSE 流式。
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
}
