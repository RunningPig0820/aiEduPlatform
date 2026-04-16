package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Neo4j 健康检查 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private boolean available;
    private long responseTimeMs;
    private String message;
}
