package com.ai.edu.domain.shared.service;

/**
 * 文件存储服务接口
 * 定义文件上传、删除等通用操作
 */
public interface FileStorageService {

    /**
     * 上传文件
     *
     * @param directory 目录路径，如 "school/avatar/123"
     * @param fileName  文件名
     * @param content   文件内容
     * @param contentType 文件类型，如 "image/jpeg"
     * @return 文件访问URL
     */
    String upload(String directory, String fileName, byte[] content, String contentType);

    /**
     * 上传文件到指定对象键（幂等整写——同 objectKey 重复上传覆盖，用于对话归档等确定性整写场景）。
     *
     * @param objectKey   对象键（如 "tutoring/transcripts/1001.json"）
     * @param content     文件内容
     * @param contentType 文件类型
     * @return objectKey（调用方持久化此值，读时经 {@link #getUrl} 生成访问 URL）
     */
    String uploadToObjectKey(String objectKey, byte[] content, String contentType);

    /**
     * 删除文件
     *
     * @param fileUrl 文件URL
     */
    void delete(String fileUrl);

    /**
     * 获取文件访问URL
     *
     * @param objectKey 对象键，如 "school/avatar/123/logo.jpg"
     * @return 文件访问URL
     */
    String getUrl(String objectKey);

    /**
     * 生成短时有效的签名访问 URL（私有读对象；答疑 transcript 归档用，读时现生成避免死链接）。
     *
     * @param objectKey      对象键，如 "tutoring/transcripts/1001.json"
     * @param expiresMinutes 有效期（分钟）
     * @return 签名访问 URL
     */
    String generatePresignedUrl(String objectKey, int expiresMinutes);
}