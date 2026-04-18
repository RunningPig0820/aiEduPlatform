package com.ai.edu.domain.shared.service;

/**
 * Neo4j 连通性检查
 */
public interface Neo4jHealthChecker {

    boolean isConnected();
}
