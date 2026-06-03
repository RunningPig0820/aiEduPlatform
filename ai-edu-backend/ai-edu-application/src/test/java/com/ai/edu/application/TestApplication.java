package com.ai.edu.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 测试启动类
 * 用于集成测试
 */
@SpringBootApplication(scanBasePackages = "com.ai.edu")
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}