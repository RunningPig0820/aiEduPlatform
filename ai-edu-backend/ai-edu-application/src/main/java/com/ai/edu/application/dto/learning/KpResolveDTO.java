package com.ai.edu.application.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 题型解析响应（POST /api/kp/resolve）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpResolveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 原 label */
    private String label;

    /** 解析出的 TextbookKP URI；PENDING 为 null */
    private String uri;

    /** 命中知识点名（冗余展示） */
    private String kpLabel;

    /** 置信度 0-100 */
    private Integer confidence;

    /** RESOLVED / PENDING */
    private String status;

    /** PENDING 时的澄清候选（学科概念 label，不暴露 kp_uri），供学生"你想学哪个" */
    private List<String> candidates;
}
