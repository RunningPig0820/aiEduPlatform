package com.ai.edu.domain.learning.model.valueobject;

import lombok.Getter;

import java.util.List;

/**
 * 知识点解析结果值对象——{@code TutoringKpResolver} 管线输出。
 *
 * <p>status=RESOLVED（uri 非空，可点亮）/ PENDING（挂起，不点亮）。
 * confidence 0-100，低置信走 PENDING（不报错，由学生澄清接续）。
 * candidates 为 PENDING 时的澄清候选（学科概念 label，不暴露 kp_uri），供学生"你想学哪个"选择。
 */
@Getter
public class KpResolution {

    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_PENDING = "PENDING";

    private final String label;
    private final String uri;
    private final String kpLabel;
    private final int confidence;
    private final String status;
    private final List<String> candidates;
    /** 冷启动弱确定（首条 LLM 消歧无先验支撑，未第二信号验证）；调用方据此降级展示为待确认。 */
    private final boolean weak;

    private KpResolution(String label, String uri, String kpLabel, int confidence, String status,
                         List<String> candidates, boolean weak) {
        this.label = label;
        this.uri = uri;
        this.kpLabel = kpLabel;
        this.confidence = confidence;
        this.status = status;
        this.candidates = candidates == null ? List.of() : candidates;
        this.weak = weak;
    }

    /** 解析成功（命中 URI，数据驱动权威：镜像/题型库，非冷启动）。 */
    public static KpResolution resolved(String label, String uri, String kpLabel, int confidence) {
        return new KpResolution(label, uri, kpLabel, confidence, STATUS_RESOLVED, List.of(), false);
    }

    /** 解析成功但为冷启动弱确定（LLM 消歧无先验支撑）；调用方应降级展示为待确认而非权威结果。 */
    public static KpResolution resolvedWeak(String label, String uri, String kpLabel, int confidence) {
        return new KpResolution(label, uri, kpLabel, confidence, STATUS_RESOLVED, List.of(), true);
    }

    /** 挂起（未命中 / 低置信，不点亮）。 */
    public static KpResolution pending(String label) {
        return new KpResolution(label, null, null, 0, STATUS_PENDING, List.of(), false);
    }

    /** 挂起并携带澄清候选（供学生"你想学哪个"）。 */
    public static KpResolution pending(String label, List<String> candidates) {
        return new KpResolution(label, null, null, 0, STATUS_PENDING, candidates, false);
    }

    public boolean isResolved() {
        return STATUS_RESOLVED.equals(status);
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    public boolean isWeak() {
        return weak;
    }
}
