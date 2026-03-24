package com.ai.edu.domain.llm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 模型信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Provider 名称
     */
    private String provider;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 完整名称，格式: {provider}/{model}
     */
    @JsonProperty("full_name")
    private String fullName;

    /**
     * 显示名称
     */
    @JsonProperty("display_name")
    private String displayName;

    /**
     * 是否免费
     */
    private Boolean free;

    /**
     * 是否支持工具调用
     */
    @JsonProperty("supports_tools")
    private Boolean supportsTools;

    /**
     * 是否支持视觉
     */
    @JsonProperty("supports_vision")
    private Boolean supportsVision;

    /**
     * 模型描述
     */
    private String description;
}