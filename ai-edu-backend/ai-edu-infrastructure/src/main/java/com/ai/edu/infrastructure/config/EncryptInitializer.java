package com.ai.edu.infrastructure.config;

import com.ai.edu.common.util.EncryptUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 加密工具初始化器
 *
 * 在 Spring 容器启动时，读取 AES 密钥并初始化 EncryptUtil。
 */
@Slf4j
@Component
public class EncryptInitializer {

    @Resource
    private EncryptProperties encryptProperties;

    @PostConstruct
    public void init() {
        String aesKey = encryptProperties.getAesKey();
        if (aesKey == null || aesKey.isBlank()) {
            log.warn("app.encrypt.aes-key 未配置，EncryptUtil 未初始化。身份证加密功能将不可用。");
            return;
        }
        EncryptUtil.init(aesKey);
        log.info("EncryptUtil 初始化成功");
    }
}
