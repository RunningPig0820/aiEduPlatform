package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * @author 张敏
 * @date 2026-04-23 20:46
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionNode {

    @Serial
    private static final long serialVersionUID = 1L;

    private String uri;
    private String label;
    private Integer orderIndex;
    private Integer knowledgePointCount;
}
