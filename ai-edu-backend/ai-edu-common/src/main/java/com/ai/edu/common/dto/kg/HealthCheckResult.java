package com.ai.edu.common.dto.kg;

/**
 * Neo4j 健康检查结果
 */
public class HealthCheckResult {
    public final boolean healthy;
    public final long responseTimeMs;
    public final String message;

    public HealthCheckResult(boolean healthy, long responseTimeMs, String message) {
        this.healthy = healthy;
        this.responseTimeMs = responseTimeMs;
        this.message = message;
    }
}
