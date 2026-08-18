package com.ai.edu.domain.learning.model.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 题型名向量最近邻（Python → Java，query 响应 {@code vectors[]} 元素）。
 *
 * <p>spike 实测契约：cosine <b>distance（越小越相似）</b>——self ≈ 0、同型 ~0.077、
 * 异型 ≥0.33；归并阈值后端收口（默认 0.2，保守宁可拆不误并）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicVectorNeighbor implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 命中的向量 key */
    private String key;

    /** 命中向量的 metadata（含 canonical_label） */
    private TopicVectorMetadata metadata;

    /** 余弦距离（越小越相似），null = Python 未返回 */
    private Double distance;
}
