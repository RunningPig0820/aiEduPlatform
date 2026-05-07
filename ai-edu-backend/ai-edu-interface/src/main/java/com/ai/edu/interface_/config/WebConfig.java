package com.ai.edu.interface_.config;

import com.ai.edu.interface_.security.SchoolPermissionInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private SchoolPermissionInterceptor schoolPermissionInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加学校权限拦截器
        // 拦截包含 schoolId 的操作路径
        registry.addInterceptor(schoolPermissionInterceptor)
                .addPathPatterns("/api/schools/{schoolId}/**")
                .excludePathPatterns(
                        "/api/schools/list",       // 列表不需要校验
                        "/api/schools/create",     // 创建不需要校验
                        "/api/users/**",           // 用户学校列表由业务逻辑处理
                        "/api/unauth/**",          // 无需认证
                        "/api/auth/**"             // 已认证路径由 Spring Security 处理
                );
    }
}