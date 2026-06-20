package com.ai.edu.interfaces.test.config;

import com.ai.edu.common.util.EncryptUtil;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 测试配置
 */
@TestConfiguration
public class TestConfig {

    @Bean
    public RedissonClient redissonClient() {
        // Mock RedissonClient bean，避免 Redis 依赖错误
        return org.mockito.Mockito.mock(RedissonClient.class);
    }

    @PostConstruct
    public void initEncryptUtil() {
        // 初始化加密工具（测试用 16 字节密钥）
        EncryptUtil.init("EduPlatform@2026");
    }
}