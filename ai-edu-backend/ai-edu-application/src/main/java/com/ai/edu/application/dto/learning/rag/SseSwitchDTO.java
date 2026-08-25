package com.ai.edu.application.dto.learning.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * SSE switch 事件数据（前端契约，camelCase）。
 *
 * <p>功能切换：intent 判定 switch_detected 后发（from → to），重置上下文按新锚点继续。不做生成中切换。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseSwitchDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 原锚定模块 id */
    private String fromAnchor;

    /** 新锚定模块 id */
    private String toAnchor;
}
