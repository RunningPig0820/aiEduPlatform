package com.ai.edu.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 COS 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "qcloud.cos")
public class CosProperties {

    /**
     * COS endpoint，如 cos.ap-guangzhou.myqcloud.com
     */
    private String endpoint;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * AccessKey ID
     */
    private String accessKeyId;

    /**
     * AccessKey Secret
     */
    private String accessKeySecret;

    /**
     * 地域，如 ap-guangzhou
     */
    private String region;

    /**
     * 访问基础URL
     */
    private String baseUrl;
}