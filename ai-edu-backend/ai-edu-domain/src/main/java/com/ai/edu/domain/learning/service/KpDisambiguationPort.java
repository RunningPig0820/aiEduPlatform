package com.ai.edu.domain.learning.service;

import com.ai.edu.domain.learning.model.valueobject.KpResolution;

/**
 * 知识点 LLM 消歧端口——冷启动两段式：LLM 生成候选知识点名 + 镜像校验（防幻觉）。
 *
 * <p>解析管线③（冷启动消歧）与维护闭环（重判）共用本端口（DRY）。纯消歧，不写 obs。
 * 实现位于 Infrastructure 层（{@code KpLlmDisambiguator}）。
 */
public interface KpDisambiguationPort {

    /**
     * LLM 消歧：① LLM 生成候选知识点名（给定题型 label + 年级）→ ② 镜像 exact/LIKE 校验。
     *
     * @return 单候选命中 → RESOLVED（置信度）；多候选命中 → PENDING 携带候选（弹澄清卡）；
     *         零命中 / LLM 失败 → null（降级挂起）
     */
    KpResolution disambiguate(String label, Integer grade);
}
