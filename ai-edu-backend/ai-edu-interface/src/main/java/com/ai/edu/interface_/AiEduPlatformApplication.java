package com.ai.edu.interface_;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.modulith.Modulith;

/**
 * AI教育平台启动类
 */
@SpringBootApplication
@Modulith
@MapperScan("com.ai.edu.infrastructure.persistence.mapper")
@EnableJpaRepositories(basePackages = "com.ai.edu.infrastructure.persistence.jpa")
@EntityScan(basePackages = "com.ai.edu.domain")
@ComponentScan(basePackages = "com.ai.edu")
public class AiEduPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiEduPlatformApplication.class, args);
    }
}