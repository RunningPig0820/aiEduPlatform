package com.ai.edu.interface_;

import com.ai.edu.domain.shared.service.RedisService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
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