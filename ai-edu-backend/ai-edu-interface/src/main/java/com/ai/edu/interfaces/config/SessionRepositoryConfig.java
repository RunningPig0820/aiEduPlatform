package com.ai.edu.interfaces.config;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.RedisSessionRepository;

/**
 * 用 BeanPostProcessor 把 Spring Boot 自动装配的 {@link RedisSessionRepository} 包一层
 * {@link SafeLogoutSessionRepository}。
 *
 * <p>保留自动配置的全部行为（命名空间/超时/flush/save 模式等，随 application.yml 变化），
 * 只给 {@code save()} 加"忽略已失效会话"的容错（见 {@link SafeLogoutSessionRepository}）。
 * 用 static @Bean 保证该 BeanPostProcessor 在其它 Bean 创建前注册。
 */
@Configuration
public class SessionRepositoryConfig {

    @Bean
    static BeanPostProcessor safeSessionRepositoryPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof RedisSessionRepository sessionRepository) {
                    return new SafeLogoutSessionRepository(sessionRepository);
                }
                return bean;
            }
        };
    }
}
