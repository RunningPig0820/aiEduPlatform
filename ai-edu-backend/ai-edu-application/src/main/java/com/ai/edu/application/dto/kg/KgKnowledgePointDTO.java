package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识点 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KgKnowledgePointDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String uri;
    private String label;
    private String difficulty;
    private String importance;
    private String cognitiveLevel;
    private String status;
}
