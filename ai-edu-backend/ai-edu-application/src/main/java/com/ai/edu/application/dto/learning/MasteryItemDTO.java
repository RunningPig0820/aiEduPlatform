package com.ai.edu.application.dto.learning;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学生知识点掌握度单项（图谱叠加：按 kp_key URI 匹配节点渲染）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasteryItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 知识点 key（TextbookKP URI） */
    private String kpKey;

    /** 知识点名（冗余展示） */
    private String kpLabel;

    /** 掌握度分值 0-100 */
    private Integer masteryLevel;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
