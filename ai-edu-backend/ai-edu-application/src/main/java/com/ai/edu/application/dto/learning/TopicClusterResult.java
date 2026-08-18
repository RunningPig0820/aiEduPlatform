package com.ai.edu.application.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 题型批量聚集结果（POST /api/kp/aggregation/topic-cluster 响应）。
 *
 * <p>手动触发（非定时）：扫描未归并题型名 → 全量聚集补归并 → 重算掌握表（幂等）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicClusterResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 扫描到的未归并题型名数量 */
    private int pendingTopics;

    /** 归并数（canonical 有变化的题型名；建锚不计入） */
    private int mergedTopics;

    public static TopicClusterResult of(int pendingTopics, int mergedTopics) {
        return TopicClusterResult.builder()
                .pendingTopics(pendingTopics)
                .mergedTopics(mergedTopics)
                .build();
    }
}
