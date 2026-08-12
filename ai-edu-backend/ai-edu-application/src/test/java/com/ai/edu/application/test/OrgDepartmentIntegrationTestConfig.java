package com.ai.edu.application.test;

import com.ai.edu.application.service.org.DepartmentAppService;
import com.ai.edu.infrastructure.config.MybatisPlusConfig;
import com.ai.edu.infrastructure.persistence.organization.repository.DepartmentRepositoryImpl;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 组织域-部门 集成测试专用配置（H2 内存库）。
 *
 * <p>与 {@code TestInfrastructureConfig} 同理：不扫描整个 com.ai.edu，
 * 只装配 {@link DepartmentAppService} 的完整依赖链（DepartmentRepositoryImpl + DepartmentMapper），
 * 避免把 Redis/Redisson/Neo4j/LLM 等外部依赖拉进测试上下文。
 * schema 建表由测试类上的 {@code @Sql(classpath:schema.sql)} 完成（跑在 primary=org 数据源）。
 */
@SpringBootConfiguration
@EnableTransactionManagement
@ImportAutoConfiguration({
    DynamicDataSourceAutoConfiguration.class,
    MybatisPlusAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    TransactionAutoConfiguration.class
})
@MapperScan("com.ai.edu.infrastructure.persistence.organization.mapper")
@Import({
    MybatisPlusConfig.class,          // 分页插件：缺失时 Page.getTotal() 恒为 0
    DepartmentAppService.class,
    DepartmentRepositoryImpl.class
})
public class OrgDepartmentIntegrationTestConfig {
}
