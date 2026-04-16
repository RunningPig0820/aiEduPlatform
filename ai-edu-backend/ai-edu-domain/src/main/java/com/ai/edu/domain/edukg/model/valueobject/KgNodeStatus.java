package com.ai.edu.domain.edukg.model.valueobject;

import lombok.Getter;

/**
 * 知识图谱节点状态枚举
 */
@Getter
public enum KgNodeStatus {

    ACTIVE("active", "正常"),
    DELETED("deleted", "已删除"),
    MERGED("merged", "已合并");

    private final String value;
    private final String label;

    KgNodeStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public static KgNodeStatus fromValue(String value) {
        for (KgNodeStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return ACTIVE;
    }
}
