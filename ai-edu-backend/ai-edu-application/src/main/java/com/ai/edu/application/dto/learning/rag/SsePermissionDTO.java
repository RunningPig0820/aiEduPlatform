package com.ai.edu.application.dto.learning.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * SSE permission 事件数据（前端契约，camelCase）。
 *
 * <p>角色门结果，仅 Java 网关产出；traceId 由 Java 入口生成，前端流开始即可取（断线补查不依赖 done）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SsePermissionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前角色（正常流恒为 STUDENT） */
    private String role;

    /** 是否放行（学生 true） */
    private Boolean allowed;

    /** 本轮 trace id（Java 生成，供断线补查） */
    private String traceId;
}
