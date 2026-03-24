package com.ai.edu.domain.shared.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 允许调用的模型列表响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllowedModelsResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 允许调用的模型列表
     */
    @JsonProperty("allowed_models")
    private List<ModelInfo> allowedModels;

    /**
     * 默认模型
     */
    @JsonProperty("default_model")
    private String defaultModel;
}