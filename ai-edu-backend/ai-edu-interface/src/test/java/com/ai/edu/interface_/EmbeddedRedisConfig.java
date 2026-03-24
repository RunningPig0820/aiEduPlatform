package com.ai.edu.interface_;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * 测试环境 Redis Mock 配置
 *
 * @author AI Edu Platform
 */
@TestConfiguration
@Import(MockRedisService.class)
public class EmbeddedRedisConfig {
}