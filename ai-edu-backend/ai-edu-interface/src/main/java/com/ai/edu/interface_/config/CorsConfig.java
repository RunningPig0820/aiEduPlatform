package com.ai.edu.interface_.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS 跨域配置
 */
@Configuration
public class CorsConfig {

    /**
     * CORS 配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的前端域名（开发环境）
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "http://localhost:5173",  // Vite 默认端口
                "http://127.0.0.1:5173"
        ));

        // 允许的 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // 允许的请求头
        configuration.setAllowedHeaders(List.of("*"));

        // 允许携带凭证（Cookie）
        configuration.setAllowCredentials(true);

        // 预检请求缓存时间（秒）
        configuration.setMaxAge(3600L);

        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList(
                "X-Request-Id",
                "X-Response-Time"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}