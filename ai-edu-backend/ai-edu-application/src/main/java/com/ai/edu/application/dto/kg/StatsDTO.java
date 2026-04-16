package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 年级知识点统计 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String grade;
    private int totalKnowledgePoints;
    private int totalTextbooks;
    private int totalChapters;
    private int totalSections;
    private Map<String, Integer> difficultyDistribution;
}
