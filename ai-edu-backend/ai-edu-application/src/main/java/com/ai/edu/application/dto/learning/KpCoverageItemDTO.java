package com.ai.edu.application.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识点派生覆盖度单项（掌握度主体翻转：知识点覆盖度由题型掌握度派生）。
 *
 * <p>coverage 连续值 0-75（详情用），masteryLevel 离散四档 0/25/50/75（列表/图谱着色用），
 * 两者都返回。kpLabel 从 kg 镜像反查，stage/chapterLabel/sectionLabel 从 kp 归属教材反查。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpCoverageItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 知识点 URI（图谱节点匹配键） */
    private String kpUri;

    /** 知识点名（kg 镜像反查） */
    private String kpLabel;

    /** 派生覆盖度 0-75（连续值，详情用） */
    private Integer coverage;

    /** 离散四档 0/25/50/75（列表/图谱着色用） */
    private Integer masteryLevel;

    /** 解析状态：RESOLVED（确定）/ PENDING（疑似待确认） */
    private String status;

    /** 覆盖该 kp 的题型中最高置信度 */
    private Integer confidence;

    /** 学段 primary/middle/high（无归属为 null） */
    private String stage;

    /** 归属章节名（无归属为 null） */
    private String chapterLabel;

    /** 归属小节名（无归属为 null） */
    private String sectionLabel;
}
