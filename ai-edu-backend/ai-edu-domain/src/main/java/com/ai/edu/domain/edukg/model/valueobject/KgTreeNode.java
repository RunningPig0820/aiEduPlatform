package com.ai.edu.domain.edukg.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 教材树浏览节点——知识地图点击式下钻每层的扁平投影（uri + label + orderIndex）。
 *
 * <p>跨 4 层复用（课本/章节/小节/知识点），由 Mapper 直接映射（列别名对齐 uri/label/orderIndex）。
 * 替代原 7 表 JOIN 分页投影 {@link KgKpPlacement}（每层单次查询，≤2 表 JOIN）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KgTreeNode {

    /** 节点 URI（kg 镜像主键） */
    private String uri;

    /** 节点展示名 */
    private String label;

    /** 同级排序（order_index，0 起） */
    private Integer orderIndex;
}
