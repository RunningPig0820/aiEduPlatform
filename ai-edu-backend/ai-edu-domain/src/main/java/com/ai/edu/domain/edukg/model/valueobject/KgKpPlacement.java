package com.ai.edu.domain.edukg.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识点归属投影值对象——kp 反查教材学段 + 章节/小节（跨 4 表 LEFT JOIN 的扁平投影）。
 *
 * <p>掌握度/知识点总览按 kpKey(URI) 反查归属：kp→section→chapter→textbook(stage)。
 * 一个 kp 可能挂多个 section，取首个非空 stage（跨教材同 kp 罕见）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KgKpPlacement {

    /** 知识点 URI */
    private String kpUri;

    /** 知识点名（kg 镜像 label 冗余投影） */
    private String kpLabel;

    /** 学段 primary/middle/high（KgTextbook.stage，对齐 KgStageEnum code） */
    private String stage;

    /** 归属章节名（无归属为 null） */
    private String chapterLabel;

    /** 归属小节名（无归属为 null） */
    private String sectionLabel;
}
