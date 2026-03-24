package com.ai.edu.application.dto.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型信息 DTO
 *
 * @author AI Edu Platform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelInfo {

    /**
     * 提供商标识
     * 例如: openai, anthropic, google
     */
    private String provider;

    /**
     * 模型标识
     * 例如: gpt-4, claude-3-opus
     */
    private String model;

    /**
     * 模型全名
     * 例如: openai/gpt-4
     */
    private String fullName;

    /**
     * 模型显示名称
     * 例如: GPT-4
     */
    private String displayName;

    /**
     * 是否免费
     */
    private Boolean free;

    /**
     * 是否支持工具调用
     */
    private Boolean supportsTools;

    /**
     * 是否支持视觉（图片）输入
     */
    private Boolean supportsVision;

    /**
     * 模型描述
     */
    private String description;
}