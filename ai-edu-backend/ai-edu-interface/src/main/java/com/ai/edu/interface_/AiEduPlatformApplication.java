package com.ai.edu.interface_;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;

/**
 * AI教育平台启动类
 */
@SpringBootApplication
@Modulith
@MapperScan("com.ai.edu.infrastructure.persistence.mapper")
public class AiEduPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiEduPlatformApplication.class, args);
    }
}