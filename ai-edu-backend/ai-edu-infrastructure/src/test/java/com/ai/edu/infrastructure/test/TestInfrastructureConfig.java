package com.ai.edu.infrastructure.test;

import com.ai.edu.domain.shared.service.RedisService;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration;
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
 * 测试用 Spring Boot 配置
 * 加载 MyBatis-Plus + Dynamic DataSource + Neo4j
 *
 * <p>本配置仅供 7 个 Kg* 仓储集成测试使用（H2 内存库跑真实 SQL），
 * 因此组件扫描收窄到 edukg 持久化包——仅实例化 Kg 仓储实现，
 * 避免把 learning/organization/user/integration 等无关实现拉进上下文。
 *
 * <p>必须引入 {@link AopAutoConfiguration}：动态数据源 {@code @DS} 拦截基于 Spring AOP advisor，
 * 缺省时 mapper 的 {@code @DS("kg")} 不生效、落到主库——由于 kg 数据源与主库共用 testdb，
 * 路由失效测试也会通过，会把 @DS 路由失败静默掩盖掉。
 */
@SpringBootConfiguration
@ImportAutoConfiguration({
    DynamicDataSourceAutoConfiguration.class,
    MybatisPlusAutoConfiguration.class,
    AopAutoConfiguration.class
})
@MapperScan({
    "com.ai.edu.infrastructure.persistence.edukg.mapper",
    "com.ai.edu.infrastructure.persistence.mapper"
})
@ComponentScan(basePackages = "com.ai.edu.infrastructure.persistence.edukg")
public class TestInfrastructureConfig {

    @Bean
    public RedisService redisService() {
        RedisService mock = org.mockito.Mockito.mock(RedisService.class);
        when(mock.get(anyString())).thenReturn(null);
        when(mock.hasKey(anyString())).thenReturn(false);
        return mock;
    }

    /**
     * 应用启动后初始化 H2 表结构
     * ApplicationRunner 在所有 Bean 初始化完成后执行，避免循环依赖
     */
    @Bean
    public ApplicationRunner schemaInitializer(DataSource dataSource) {
        return args -> {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("schema.sql"));
            populator.setContinueOnError(false);
            populator.execute(dataSource);
        };
    }
}
