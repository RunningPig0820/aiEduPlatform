package com.ai.edu.application.dto.learning.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * SSE rerank 事件数据（前端契约，camelCase）。
 *
 * <p>RRF 精排 Top-K 块（仅回传精排块，不吐全量召回），供引用面板灰显。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseRerankDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 精排 Top-K 块（blockId/title/summary/filePath/score） */
    private List<SseRerankBlock> blocks;
}
