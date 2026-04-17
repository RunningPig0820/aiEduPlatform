package com.ai.edu.infrastructure.test;

import com.ai.edu.domain.shared.service.RedisService;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 测试用 Spring Boot 配置
 * 加载 MyBatis-Plus + Dynamic DataSource + Neo4j
 */
@SpringBootConfiguration
@ImportAutoConfiguration({
    DynamicDataSourceAutoConfiguration.class,
    MybatisPlusAutoConfiguration.class
})
@MapperScan({
    "com.ai.edu.infrastructure.persistence.edukg.mapper",
    "com.ai.edu.infrastructure.persistence.mapper"
})
@ComponentScan(
    basePackages = "com.ai.edu.infrastructure",
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.ai\\.edu\\.infrastructure\\.ai\\..*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.ai\\.edu\\.infrastructure\\.cache\\..*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.ai\\.edu\\.infrastructure\\.file\\..*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.ai\\.edu\\.infrastructure\\.mq\\..*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.ai\\.edu\\.infrastructure\\.security\\..*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.ai\\.edu\\.infrastructure\\.redis\\..*"),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
            com.ai.edu.infrastructure.neo4j.Neo4jRelationQueryService.class
        })
    }
)
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
