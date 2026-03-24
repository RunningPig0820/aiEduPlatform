package com.ai.edu.application.dto.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 允许调用的模型列表响应 DTO
 *
 * @author AI Edu Platform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllowedModelsResponse {

    /**
     * 允许调用的模型列表
     */
    private List<ModelInfo> allowedModels;

    /**
     * 默认模型
     */
    private String defaultModel;
}