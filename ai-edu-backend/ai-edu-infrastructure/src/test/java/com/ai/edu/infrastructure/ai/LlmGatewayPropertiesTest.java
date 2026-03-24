package com.ai.edu.infrastructure.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Field;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LlmGatewayProperties 配置类测试
 *
 * 测试配置属性绑定是否正确
 */
class LlmGatewayPropertiesTest {

    @Test
    void shouldHaveConfigurationPropertiesAnnotation() {
        // 验证类上有 @ConfigurationProperties 注解
        ConfigurationProperties annotation = AnnotationUtils.findAnnotation(
                LlmGatewayProperties.class, ConfigurationProperties.class);
        assertNotNull(annotation, "LlmGatewayProperties 应该有 @ConfigurationProperties 注解");
        assertEquals("ai-edu.llm.gateway", annotation.prefix(),
                "配置前缀应该是 ai-edu.llm.gateway");
    }

    @Test
    void shouldHaveAllRequiredFields() throws NoSuchFieldException {
        // 验证所有必需的字段都存在
        Class<LlmGatewayProperties> clazz = LlmGatewayProperties.class;

        Field baseUrlField = clazz.getDeclaredField("baseUrl");
        assertEquals(String.class, baseUrlField.getType(), "baseUrl 应该是 String 类型");

        Field internalTokenField = clazz.getDeclaredField("internalToken");
        assertEquals(String.class, internalTokenField.getType(), "internalToken 应该是 String 类型");

        Field connectTimeoutField = clazz.getDeclaredField("connectTimeout");
        assertEquals(Duration.class, connectTimeoutField.getType(), "connectTimeout 应该是 Duration 类型");

        Field readTimeoutField = clazz.getDeclaredField("readTimeout");
        assertEquals(Duration.class, readTimeoutField.getType(), "readTimeout 应该是 Duration 类型");
    }

    @Test
    void shouldSetAndGetProperties() {
        // 验证 setter 和 getter 工作正常
        LlmGatewayProperties properties = new LlmGatewayProperties();
        Duration connectTimeout = Duration.ofSeconds(10);
        Duration readTimeout = Duration.ofSeconds(60);

        properties.setBaseUrl("http://localhost:9527");
        properties.setInternalToken("test-token");
        properties.setConnectTimeout(connectTimeout);
        properties.setReadTimeout(readTimeout);

        assertEquals("http://localhost:9527", properties.getBaseUrl());
        assertEquals("test-token", properties.getInternalToken());
        assertEquals(connectTimeout, properties.getConnectTimeout());
        assertEquals(readTimeout, properties.getReadTimeout());
    }

    @Test
    void shouldHaveDefaultValues() {
        // 验证默认值
        LlmGatewayProperties properties = new LlmGatewayProperties();

        assertNull(properties.getBaseUrl(), "baseUrl 默认值应该是 null");
        assertNull(properties.getInternalToken(), "internalToken 默认值应该是 null");
        assertNull(properties.getConnectTimeout(), "connectTimeout 默认值应该是 null");
        assertNull(properties.getReadTimeout(), "readTimeout 默认值应该是 null");
    }
}