package com.ai.edu.infrastructure.persistence.neo4j;

import com.ai.edu.domain.shared.service.Neo4jHealthChecker;
import jakarta.annotation.Resource;
import org.neo4j.driver.Driver;
import org.springframework.stereotype.Component;

/**
 * Neo4j 健康检查实现
 */
@Component
public class Neo4jHealthCheckerImpl implements Neo4jHealthChecker {

    @Resource
    private Driver neo4jDriver;

    @Override
    public boolean isConnected() {
        try (var session = neo4jDriver.session()) {
            session.readTransaction(tx -> {
                tx.run("RETURN 1").single();
                return null;
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
