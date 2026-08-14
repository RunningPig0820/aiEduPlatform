package com.ai.edu.domain.learning.service;

import com.ai.edu.domain.learning.model.valueobject.KpResolution;

/**
 * 知识点 label → TextbookKP URI 解析端口。
 *
 * <p>解析管线（见实现 {@code TutoringKpResolverImpl}）：
 * ① kg-sync 镜像精确/LIKE → ② 题型库年级匹配 → ③ LLM 消歧 → ④ 低置信挂起。
 * 解析失败不影响答疑主流程（PENDING 不报错）。
 *
 * <p>实现位于 Infrastructure 层（{@code TutoringKpResolverImpl}）。
 */
public interface TutoringKpResolver {

    /**
     * label → 解析结果（URI + 置信度 + 状态）。
     *
     * @param label     知识点/题型原文（Python mastery_signals / eval 输出的 label）
     * @param studentId 学生ID（查年级做年级锚；null 则无年级锚，降级纯 LLM 消歧）
     * @return 解析结果；低置信/歧义返回 {@code status=PENDING}
     */
    KpResolution resolve(String label, Long studentId);

    /**
     * 学生澄清投票：学生从澄清候选中选择归属知识点，落 source=student_vote 观测。
     *
     * @param topicLabel    题型/知识点原文
     * @param studentId     学生ID
     * @param selectedLabel 学生选择的候选学科概念（须为 resolve 返回的 candidate 之一）
     */
    void recordStudentVote(String topicLabel, Long studentId, String selectedLabel);

    /**
     * 兼容旧调用：label → TextbookKP URI（无年级锚，镜像命中行为不变）。
     *
     * @param label 知识点名
     * @return TextbookKP URI；未命中返回 {@code null}
     */
    default String resolveLabelToUri(String label) {
        return resolve(label, null).getUri();
    }
}
