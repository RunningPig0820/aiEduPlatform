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
}