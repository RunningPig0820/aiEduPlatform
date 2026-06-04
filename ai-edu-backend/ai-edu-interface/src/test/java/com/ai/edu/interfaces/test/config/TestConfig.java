package com.ai.edu.interfaces.test.config;

import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 测试配置
 * 在测试环境中使用 H2UserQueryService 而不是 MockUserQueryService
 */
@TestConfiguration
public class TestConfig {

    @Bean
    public RedissonClient redissonClient() {
        // Mock RedissonClient bean，避免 Redis 依赖错误
        return org.mockito.Mockito.mock(RedissonClient.class);
    }
}