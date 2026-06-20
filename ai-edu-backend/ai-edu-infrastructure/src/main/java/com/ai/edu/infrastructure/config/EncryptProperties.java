package com.ai.edu.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 加密配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.encrypt")
public class EncryptProperties {

    /**
     * AES 加密密钥（16/24/32 字节）
     * 支持通过环境变量 APP_ENCRYPT_AES_KEY 覆盖
     */
    private String aesKey;
}
