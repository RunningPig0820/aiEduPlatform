package com.ai.edu.application.dto.learning.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * rerank 精排块（前端契约，camelCase）。
 *
 * <p>RRF 精排 Top-K 块：标题/摘要/filePath 供引用面板灰显展示（filePath 可点"查看原文"）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseRerankBlock implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 块 id（is_quoted 命中时进入 quotedKeys） */
    private String blockId;

    /** 块标题 */
    private String title;

    /** 块摘要 */
    private String summary;

    /** 原文路径（点击"查看原文"走 Java 代理 source 端点） */
    private String filePath;

    /** RRF 综合分 */
    private Double score;
}
