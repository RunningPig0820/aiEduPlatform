package com.ai.edu.interface_.test.config;

import com.ai.edu.infrastructure.integration.user.H2UserQueryService;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 测试配置
 * 在测试环境中使用 H2UserQueryService 而不是 MockUserQueryService
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public H2UserQueryService h2UserQueryService(DataSource dataSource) {
        // 使用 primary 数据源（user数据源）
        return new H2UserQueryService(dataSource);
    }

    @Bean
    public RedissonClient redissonClient() {
        // Mock RedissonClient bean，避免 Redis 依赖错误
        return org.mockito.Mockito.mock(RedissonClient.class);
    }
}