package com.ai.edu.infrastructure.file.impl;

import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.shared.service.FileStorageService;
import com.ai.edu.infrastructure.config.CosProperties;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.UUID;

/**
 * 腾讯云 COS 文件存储服务实现
 */
@Slf4j
@Service
public class CosFileStorageServiceImpl implements FileStorageService {

    @Resource
    private CosProperties cosProperties;

    public COSClient createCOSClient() {
        log.info("初始化腾讯云 COS 客户端: region={}, bucket={}", cosProperties.getRegion(), cosProperties.getBucketName());

        // 1. 初始化用户身份信息
        COSCredentials cred = new BasicCOSCredentials(cosProperties.getAccessKeyId(), cosProperties.getAccessKeySecret());

        // 2. 设置 bucket 的地域
        Region region = new Region(cosProperties.getRegion());
        ClientConfig clientConfig = new ClientConfig(region);
        clientConfig.setHttpProtocol(HttpProtocol.https);

        // 3. 生成 cos 客户端
        return new COSClient(cred, clientConfig);
    }


    @Override
    public String upload(String directory, String fileName, byte[] content, String contentType) {
        // 生成唯一文件名，避免冲突
        String uniqueFileName = generateUniqueFileName(fileName);
        String objectKey = directory + "/" + uniqueFileName;

        COSClient cosClient = createCOSClient();

        try {
            // 设置元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(content.length);

            PutObjectRequest request = new PutObjectRequest(
                    cosProperties.getBucketName(),
                    objectKey,
                    new ByteArrayInputStream(content),
                    metadata
            );

            cosClient.putObject(request);
            log.info("文件上传成功: objectKey={}, size={}bytes", objectKey, content.length);

            return getUrl(objectKey);
        } catch (Exception e) {
            log.error("文件上传失败: directory={}, fileName={}", directory, fileName, e);
            throw new BusinessException("FILE_UPLOAD_FAILED", "文件上传失败: " + e.getMessage());
        }finally {
            cosClient.shutdown();
        }
    }

    @Override
    public void delete(String fileUrl) {
        String objectKey = extractObjectKey(fileUrl);
        if (objectKey == null) {
            log.warn("无法从URL提取objectKey: {}", fileUrl);
            return;
        }

        COSClient cosClient = createCOSClient();

        try {
            cosClient.deleteObject(cosProperties.getBucketName(), objectKey);
            log.info("文件删除成功: objectKey={}", objectKey);
        } catch (Exception e) {
            log.error("文件删除失败: objectKey={}", objectKey, e);
            throw new BusinessException("FILE_DELETE_FAILED", "文件删除失败: " + e.getMessage());
        }finally {
            cosClient.shutdown();
        }
    }

    @Override
    public String getUrl(String objectKey) {
        return cosProperties.getBaseUrl() + "/" + objectKey;
    }

    /**
     * 生成唯一文件名
     */
    private String generateUniqueFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString().replace("-", "") + extension;
    }

    /**
     * 从URL中提取objectKey
     */
    private String extractObjectKey(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(cosProperties.getBaseUrl())) {
            return null;
        }
        return fileUrl.substring(cosProperties.getBaseUrl().length() + 1);
    }
}