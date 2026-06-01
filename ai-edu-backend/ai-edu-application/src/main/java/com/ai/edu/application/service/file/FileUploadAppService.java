package com.ai.edu.application.service.file;

import com.ai.edu.application.dto.file.UploadResultDTO;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.shared.service.FileStorageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * 文件上传应用服务
 */
@Slf4j
@Service
public class FileUploadAppService {

    /**
     * 最大文件大小：5MB
     */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * 允许的图片类型
     */
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png"
    );

    /**
     * 允许的图片扩展名
     */
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png"
    );

    @Resource
    private FileStorageService fileStorageService;

    /**
     * 上传学校头像
     *
     * @param schoolId 学校ID
     * @param file     图片文件
     * @return 上传结果
     */
    public UploadResultDTO uploadSchoolAvatar(Long schoolId, MultipartFile file) {
        log.info("上传学校头像: schoolId={}, fileName={}, size={}",
                schoolId, file.getOriginalFilename(), file.getSize());

        // 1. 校验文件
        validateImageFile(file);

        // 2. 构建目录路径
        String directory = "school/avatar/" + schoolId;

        try {
            // 3. 上传文件
            String url = fileStorageService.upload(
                    directory,
                    file.getOriginalFilename(),
                    file.getBytes(),
                    file.getContentType()
            );

            log.info("学校头像上传成功: schoolId={}, url={}", schoolId, url);

            return UploadResultDTO.builder()
                    .url(url)
                    .fileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .build();
        } catch (Exception e) {
            log.error("学校头像上传失败: schoolId={}", schoolId, e);
            throw new BusinessException("FILE_UPLOAD_FAILED", "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 校验图片文件
     */
    private void validateImageFile(MultipartFile file) {
        // 1. 校验文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException("FILE_EMPTY", "文件不能为空");
        }

        // 2. 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("FILE_TOO_LARGE", "文件大小不能超过5MB");
        }

        // 3. 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("FILE_TYPE_NOT_ALLOWED", "只支持 jpg, png 格式的图片");
        }

        // 4. 校验文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException("FILE_NAME_EMPTY", "文件名不能为空");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new BusinessException("FILE_EXTENSION_NOT_ALLOWED", "只支持 jpg, png 格式的图片");
        }
    }
}