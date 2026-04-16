package com.ai.edu.infrastructure.persistence.neo4j;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.SessionConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Neo4j Driver 配置类
 *
 * @author AI Edu Platform
 */
@Slf4j
@Configuration
public class Neo4jConfig {

    @Value("${spring.neo4j.uri}")
    private String uri;

    @Value("${spring.neo4j.username}")
    private String username;

    @Value("${spring.neo4j.password}")
    private String password;

    @Value("${spring.neo4j.max-connection-pool-size:50}")
    private int maxConnectionPoolSize;

    @Value("${spring.neo4j.connection-timeout:30s}")
    private Duration connectionTimeout;

    @Value("${spring.neo4j.connection-acquisition-timeout:60s}")
    private Duration connectionAcquisitionTimeout;

    @Bean
    public Driver neo4jDriver() {
        log.info("Initializing Neo4j Driver with uri: {}", uri);

        Config config = Config.builder()
                .withMaxConnectionPoolSize(maxConnectionPoolSize)
                .withConnectionTimeout(connectionTimeout.getSeconds(), TimeUnit.SECONDS)
                .build();

        Driver driver = GraphDatabase.driver(
                uri,
                AuthTokens.basic(username, password),
                config
        );

        // 验证连接
        try {
            driver.verifyConnectivity();
            log.info("Neo4j connection verified successfully");
        } catch (Exception e) {
            log.error("Failed to verify Neo4j connectivity: {}", e.getMessage());
        }

        return driver;
    }

    @Bean
    public SessionConfig neo4jSessionConfig() {
        return SessionConfig.forDatabase("neo4j");
    }
}
