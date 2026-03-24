package com.ai.edu.infrastructure.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * LLM Gateway 配置属性
 */
@Data
@ConfigurationProperties(prefix = "ai-edu.llm.gateway")
public class LlmGatewayProperties {

    /**
     * Gateway 基础 URL
     */
    private String baseUrl;

    /**
     * 内部认证 Token
     */
    private String internalToken;

    /**
     * 连接超时时间
     */
    private Duration connectTimeout;

    /**
     * 读取超时时间
     */
    private Duration readTimeout;
}