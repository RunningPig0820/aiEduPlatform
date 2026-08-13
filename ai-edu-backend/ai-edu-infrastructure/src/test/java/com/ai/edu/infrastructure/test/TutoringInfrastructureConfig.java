package com.ai.edu.infrastructure.test;

import com.ai.edu.domain.shared.service.RedisService;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 测试用 Spring Boot 配置（答疑会话 H2 集成测试专用）。
 *
 * <p>与 {@link TestInfrastructureConfig} 互不影响：本配置只实例化 learning 持久化包
 * （答疑会话仓储/映射），避免把 edukg/organization/user 等无关实现拉进上下文。
 *
 * <p>learning 数据源是独立 H2 库（testdb_learning，与主库/edukg 共享库分离），
 * 表结构经 ApplicationRunner 借路由上下文（push "learning"）应用到该库。
 * 逻辑删除配置（logic-delete-field: deleted）由测试类 {@code @TestPropertySource} 注入，
 * 保持共享 application-h2.yml 不变，从而不影响现有 Kg* H2 测试。
 *
 * <p>必须引入 {@link AopAutoConfiguration}：动态数据源 {@code @DS} 的注解拦截基于 Spring AOP
 * advisor，缺省时 mapper 的 {@code @DS("learning")} 不生效，会落到主库而非 learning 库。
 */
@SpringBootConfiguration
@ImportAutoConfiguration({
    DynamicDataSourceAutoConfiguration.class,
    MybatisPlusAutoConfiguration.class,
    AopAutoConfiguration.class
})
@MapperScan("com.ai.edu.infrastructure.persistence.learning.mapper")
@ComponentScan(basePackages = "com.ai.edu.infrastructure.persistence.learning")
public class TutoringInfrastructureConfig {

    @Bean
    public RedisService redisService() {
        RedisService mock = org.mockito.Mockito.mock(RedisService.class);
        when(mock.get(anyString())).thenReturn(null);
        when(mock.hasKey(anyString())).thenReturn(false);
        return mock;
    }

    /**
     * 在 learning 数据源上初始化答疑表结构。
     * 注入的 DataSource 为路由数据源（DynamicRoutingDataSource），push("learning")
     * 后其 getConnection() 返回 learning 库连接，脚本即建在该库。
     */
    @Bean
    public ApplicationRunner learningSchemaInitializer(DataSource dataSource) {
        return args -> {
            DynamicDataSourceContextHolder.push("learning");
            try {
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.addScript(new ClassPathResource("schema-learning.sql"));
                populator.setContinueOnError(false);
                populator.execute(dataSource);
            } finally {
                DynamicDataSourceContextHolder.poll();
            }
        };
    }
}
