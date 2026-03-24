package com.ai.edu.domain.llm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 场景列表响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenesResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 场景列表
     */
    private List<SceneInfo> scenes;
}