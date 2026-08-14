package com.ai.edu.domain.learning.service;

import com.ai.edu.domain.learning.model.valueobject.KpResolution;

/**
 * 知识点 LLM 消歧端口——给定题型 label + 学生年级，从候选知识点选最匹配（选择题 + 候选内校验防幻觉）。
 *
 * <p>解析管线③（冷启动消歧）与维护闭环（重判）共用本端口（DRY）。纯消歧，不写 obs。
 * 实现位于 Infrastructure 层（{@code KpLlmDisambiguator}）。
 */
public interface KpDisambiguationPort {

    /**
     * LLM 消歧：label + 年级 → 候选（镜像 LIKE 召回）→ 选择题 → 候选内校验。
     *
     * @return 解析结果（RESOLVED + 置信度）；失败/无候选返回 null（降级挂起）
     */
    KpResolution disambiguate(String label, Integer grade);
}
