package com.ai.edu.application.dto.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 所有模型列表响应 DTO
 *
 * @author AI Edu Platform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelsResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 提供商列表
     */
    private List<ProviderInfo> providers;

    /**
     * 提供商信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 提供商标识
         */
        private String name;

        /**
         * 提供商显示名称
         */
        private String displayName;

        /**
         * 该提供商下的模型列表
         */
        private List<ModelSummary> models;
    }

    /**
     * 模型摘要信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelSummary implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 模型名称
         */
        private String model;

        /**
         * 显示名称
         */
        private String displayName;

        /**
         * 是否免费
         */
        private Boolean free;
    }
}