package com.ai.edu.interface_;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.modulith.Modulith;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * AI教育平台启动类
 */
@SpringBootApplication
@Modulith
@EnableMethodSecurity
@MapperScan({
    "com.ai.edu.infrastructure.persistence.mapper",
    "com.ai.edu.infrastructure.persistence.edukg.mapper"
})
@ComponentScan(basePackages = "com.ai.edu")
public class AiEduPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiEduPlatformApplication.class, args);
    }
}