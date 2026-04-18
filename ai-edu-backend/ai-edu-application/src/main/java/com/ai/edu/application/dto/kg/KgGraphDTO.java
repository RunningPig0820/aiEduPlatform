package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 知识图谱图数据响应 DTO
 * 用于前端图谱可视化渲染
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KgGraphDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<GraphNode> nodes;
    private List<GraphEdge> edges;

    /**
     * 图谱节点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphNode implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 节点唯一标识，如 "kg:point:xxx" */
        private String id;

        /** 节点类型：textbook_kp 或 kp */
        private String type;

        /** 节点显示名称 */
        private String label;

        /** 节点详细数据 */
        private Map<String, Object> data;
    }

    /**
     * 图谱边
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphEdge implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 边唯一标识 */
        private String id;

        /** 源节点 URI */
        private String source;

        /** 目标节点 URI */
        private String target;

        /** 边关系标签 */
        private String label;
    }
}
