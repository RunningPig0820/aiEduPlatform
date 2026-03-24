package com.ai.edu.domain.shared.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Provider 信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Provider 名称
     */
    private String name;

    /**
     * 显示名称
     */
    @JsonProperty("display_name")
    private String displayName;

    /**
     * 该 Provider 下的模型列表
     */
    private List<ModelSummary> models;

    /**
     * 模型摘要信息（用于 ModelsResponse）
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
        @JsonProperty("display_name")
        private String displayName;

        /**
         * 是否免费
         */
        private Boolean free;
    }
}