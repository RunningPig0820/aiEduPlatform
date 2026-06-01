package com.ai.edu.application.dto.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件上传结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件访问URL
     */
    private String url;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String contentType;
}