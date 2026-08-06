package com.ai.edu.application.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 收尾总结 DTO（Java 生成，结构化：涉及知识点/薄弱点）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 涉及知识点（label 列表） */
    private List<String> knowledgePoints;

    /** 薄弱点（label 列表） */
    private List<String> weakPoints;
}
