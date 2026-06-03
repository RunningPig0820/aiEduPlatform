package com.ai.edu.application;

import com.ai.edu.application.service.org.DepartmentAppService;
import com.ai.edu.domain.organization.repository.DepartmentRepository;
import com.ai.edu.infrastructure.persistence.organization.mapper.DepartmentMapper;
import com.ai.edu.infrastructure.persistence.organization.repository.DepartmentRepositoryImpl;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * 组织域测试配置
 * 只加载组织域相关的组件，排除其他服务依赖
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {
        RedisAutoConfiguration.class,
        SessionAutoConfiguration.class,
        Neo4jDataAutoConfiguration.class
})
@ComponentScan(
        basePackages = "com.ai.edu",
        includeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        DepartmentAppService.class,
                        DepartmentRepository.class,
                        DepartmentRepositoryImpl.class,
                        DepartmentMapper.class
                })
        },
        useDefaultFilters = false
)
public class OrganizationTestConfig {
}