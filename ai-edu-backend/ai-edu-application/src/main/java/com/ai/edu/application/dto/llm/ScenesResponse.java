package com.ai.edu.application.dto.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 场景列表响应 DTO
 *
 * @author AI Edu Platform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenesResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 场景列表
     */
    private List<SceneInfo> scenes;

    /**
     * 场景信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SceneInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
        /**
         * 场景代码
         */
        private String code;

        /**
         * 该场景默认提供商
         */
        private String defaultProvider;

        /**
         * 该场景默认模型
         */
        private String defaultModel;

        /**
         * 场景描述
         */
        private String description;
    }
}