package com.ai.edu.application.dto.learning.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * SSE reject 事件数据（前端契约，camelCase）。
 *
 * <p><b>遗留 DTO</b>：早期"禁区硬拒答"设计的 reject 事件，已被范围门 boundary 取代
 * （唯一拒答路径 = boundary reason=low_confidence）。当前契约<b>不产出</b> reject 事件，保留
 * 定义仅为契约冻结完整性。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseRejectDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 固定拒答话术 */
    private String message;

    /** 拒答原因 */
    private String reason;
}
