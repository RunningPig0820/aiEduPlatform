package com.ai.edu.domain.shared.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 所有模型列表响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelsResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Provider 列表
     */
    private List<ProviderInfo> providers;
}