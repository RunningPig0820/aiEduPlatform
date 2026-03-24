package com.ai.edu.domain.shared.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 场景信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SceneInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 场景代码
     */
    private String code;

    /**
     * 默认 Provider
     */
    @JsonProperty("default_provider")
    private String defaultProvider;

    /**
     * 默认模型
     */
    @JsonProperty("default_model")
    private String defaultModel;

    /**
     * 场景描述
     */
    private String description;
}