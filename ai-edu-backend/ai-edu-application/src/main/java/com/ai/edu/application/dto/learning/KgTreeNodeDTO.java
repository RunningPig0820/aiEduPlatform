package com.ai.edu.application.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 教材树浏览节点 DTO（知识地图点击式下钻每层返回：uri + label + orderIndex）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KgTreeNodeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 节点 URI（kg 镜像主键） */
    private String uri;

    /** 节点展示名（课本/章节/小节/知识点名） */
    private String label;

    /** 同级排序（order_index，0 起） */
    private Integer orderIndex;
}
