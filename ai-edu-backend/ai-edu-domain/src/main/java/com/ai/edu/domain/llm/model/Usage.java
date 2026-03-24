package com.ai.edu.domain.llm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Token 使用量统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 提示词 Token 数
     */
    @JsonProperty("prompt_tokens")
    private Long promptTokens;

    /**
     * 生成 Token 数
     */
    @JsonProperty("completion_tokens")
    private Long completionTokens;

    /**
     * 总 Token 数
     */
    @JsonProperty("total_tokens")
    private Long totalTokens;
}