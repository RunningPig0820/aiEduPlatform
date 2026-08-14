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

    private KpResolution(String label, String uri, String kpLabel, int confidence, String status,
                         List<String> candidates) {
        this.label = label;
        this.uri = uri;
        this.kpLabel = kpLabel;
        this.confidence = confidence;
        this.status = status;
        this.candidates = candidates == null ? List.of() : candidates;
    }

    /** 解析成功（命中 URI，可点亮）。 */
    public static KpResolution resolved(String label, String uri, String kpLabel, int confidence) {
        return new KpResolution(label, uri, kpLabel, confidence, STATUS_RESOLVED, List.of());
    }

    /** 挂起（未命中 / 低置信，不点亮）。 */
    public static KpResolution pending(String label) {
        return new KpResolution(label, null, null, 0, STATUS_PENDING, List.of());
    }

    /** 挂起并携带澄清候选（供学生"你想学哪个"）。 */
    public static KpResolution pending(String label, List<String> candidates) {
        return new KpResolution(label, null, null, 0, STATUS_PENDING, candidates);
    }

    public boolean isResolved() {
        return STATUS_RESOLVED.equals(status);
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }
}
