package com.ai.edu.interface_.config;

import com.ai.edu.application.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Spring Security 配置
 * 纯 API 后端，使用自定义 Controller 处理登录
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 安全过滤器链配置
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 启用 CORS
                .cors(cors -> {})
                // 禁用 CSRF（纯 API 后端）
                .csrf(AbstractHttpConfigurer::disable)
                // 配置请求授权
                .authorizeHttpRequests(auth -> auth
                        // 无需认证的接口（登录、注册、登出等）
                        .requestMatchers("/api/unauth/**").permitAll()
                        // Swagger / OpenAPI 文档
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // /api/auth/** 需要认证（知识图谱、LLM 等）
                        .requestMatchers("/api/auth/**").authenticated()
                        // 演示/健康检查/错误页面
                        .requestMatchers("/demo/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // 其他请求放行
                        .anyRequest().permitAll()
                )
                // 配置登出
                .logout(logout -> logout
                        .logoutUrl("/api/unauth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            writeJsonResponse(response, ApiResponse.success("登出成功"));
                        })
                        .permitAll()
                )
                // 未认证处理（返回 401 JSON）
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            writeJsonResponse(response, ApiResponse.error("10004", "未登录"));
                        })
                );

        return http.build();
    }

    /**
     * 写入 JSON 响应
     */
    private void writeJsonResponse(HttpServletResponse response, ApiResponse<?> data) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(data));
    }

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}