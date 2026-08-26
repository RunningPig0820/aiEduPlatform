package com.ai.edu.application.dto.learning.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 关闭对话结算（前端契约，camelCase）。
 *
 * <p>学生主动结束会话：置 session closed（Redis）+ 返回会话累计 token/轮数。
 * 幂等：已关闭也返回 closed=true。未知 session（无累计且未关闭）→ 10002。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagCloseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 会话 id */
    private String sessionId;

    /** 是否已关闭（幂等：已关闭也返回 true） */
    private Boolean closed;

    /** 会话问答轮数 */
    private Integer rounds;

    /** 会话累计 token（prompt/completion/cacheHit/total） */
    private RagSessionUsageDTO sessionUsage;
}
