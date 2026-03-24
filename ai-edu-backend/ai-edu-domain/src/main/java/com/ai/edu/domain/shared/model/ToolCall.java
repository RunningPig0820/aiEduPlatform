package com.ai.edu.domain.shared.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 工具调用信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCall implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工具调用 ID
     */
    private String id;

    /**
     * 工具类型
     */
    private String type;

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具参数
     */
    private String arguments;
}