package com.ai.edu.interfaces.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.session.SessionRepository;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 验证生产装配：store-type=redis 时，{@link SessionRepositoryConfig} 的 BeanPostProcessor
 * 把自动装配的 RedisSessionRepository 包成 {@link SafeLogoutSessionRepository}，
 * 且 Spring 仍能正常把包装后的 repository 注入 {@link SessionRepositoryFilter}。
 *
 * <p>用 test profile（H2 + store-type:none）为基础，仅覆盖 session 存储为 redis；
 * Redis 连接是懒建立的，上下文加载不需要真实 Redis 可达。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.session.store-type=redis",
        "spring.session.redis.namespace=test:session"
})
class SafeSessionRepositoryWiringTest {

    @Autowired
    private SessionRepository<?> sessionRepository;

    @Autowired(required = false)
    private SessionRepositoryFilter<?> springSessionRepositoryFilter;

    @Test
    void redisSessionRepositoryIsWrapped() {
        assertInstanceOf(SafeLogoutSessionRepository.class, sessionRepository);
    }

    @Test
    void sessionRepositoryFilterStillWired() {
        assertNotNull(springSessionRepositoryFilter, "包装后 SessionRepositoryFilter 应仍能装配");
    }
}
