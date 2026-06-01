package com.ai.edu.interface_.api;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.file.UploadResultDTO;
import com.ai.edu.application.service.file.FileUploadAppService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    @Resource
    private FileUploadAppService fileUploadAppService;

    /**
     * 上传学校头像
     *
     * @param schoolId 学校ID
     * @param file     图片文件（jpg/png，最大5MB）
     * @return 上传结果，包含图片URL
     */
    @PostMapping("/schools/{schoolId}/avatar")
    public ApiResponse<UploadResultDTO> uploadSchoolAvatar(
            @PathVariable Long schoolId,
            @RequestParam("file") MultipartFile file) {
        log.info("uploadSchoolAvatar: schoolId={}, fileName={}", schoolId, file.getOriginalFilename());
        UploadResultDTO result = fileUploadAppService.uploadSchoolAvatar(schoolId, file);
        return ApiResponse.success(result);
    }
}