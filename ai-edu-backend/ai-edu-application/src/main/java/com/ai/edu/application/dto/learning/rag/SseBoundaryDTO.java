package com.ai.edu.application.dto.learning.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * SSE boundary 事件数据（前端契约，camelCase）。
 *
 * <p>范围门低置信度过滤（唯一拒答路径），reason=low_confidence，固定话术不调 LLM。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseBoundaryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 固定话术（"未找到关联文档，我尚未掌握"） */
    private String message;

    /** 拒答原因（恒为 low_confidence） */
    private String reason;
}
