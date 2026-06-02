package com.ai.edu.interface_.api;

import cn.hutool.json.JSONUtil;
import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.file.UploadResultDTO;
import com.ai.edu.application.dto.org.command.AssociateUserWithSchoolCommand;
import com.ai.edu.application.dto.org.command.CreateSchoolCommand;
import com.ai.edu.application.dto.org.SchoolDTO;
import com.ai.edu.application.dto.org.command.UpdateSchoolCommand;
import com.ai.edu.application.dto.org.UserSchoolAssociationDTO;
import com.ai.edu.application.service.file.FileUploadAppService;
import com.ai.edu.application.service.org.OrganizationAppService;
import com.ai.edu.application.service.org.SchoolAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 学校组织API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/schools")
public class SchoolController {
    
    @Resource
    private FileUploadAppService fileUploadAppService;
    @Resource
    private OrganizationAppService organizationAppService;
    @Resource
    private SchoolAppService schoolAppService;

    /**
     * 创建学校
     */
    @PostMapping("/create")
    public ApiResponse<SchoolDTO> createSchool(@Valid @RequestBody CreateSchoolCommand command) {
        log.info("createSchool: request={}", JSONUtil.toJsonStr(command));
        SchoolDTO school = schoolAppService.createSchool(command);
        return ApiResponse.success(school);
    }

    /**
     * 更新学校
     */
    @PostMapping("/{id}/update")
    public ApiResponse<SchoolDTO> updateSchool(@PathVariable Long id, @Valid @RequestBody UpdateSchoolCommand command) {
        log.info("updateSchool: id={}, request={}", id, JSONUtil.toJsonStr(command));
        SchoolDTO school = schoolAppService.updateSchool(id, command);
        return ApiResponse.success(school);
    }

    /**
     * 获取学校详情
     */
    @GetMapping("/{id}")
    public ApiResponse<SchoolDTO> getSchool(@PathVariable Long id) {
        log.info("getSchool: id={}", id);
        SchoolDTO school = schoolAppService.getSchoolById(id);
        return ApiResponse.success(school );
    }

    /**
     * 获取学校列表
     */
    @GetMapping("/list")
    public ApiResponse<List<SchoolDTO>> listSchools(@RequestParam(required = false) String type) {
        log.info("listSchools: type={}", type);
        List<SchoolDTO> schools;
        if (type != null && !type.isBlank()) {
            schools = schoolAppService.listSchoolsByType(type);
        } else {
            schools = schoolAppService.listSchools();
        }
        return ApiResponse.success(schools);
    }

    /**
     * 删除学校
     */
    @PostMapping("/{id}/delete")
    public ApiResponse<Void> deleteSchool(@PathVariable Long id) {
        log.info("deleteSchool: id={}", id);
        schoolAppService.deleteSchool(id);
        return ApiResponse.success(null);
    }


    /**
     * 上传学校头像
     *
     * @param schoolId 学校ID
     * @param file     图片文件（jpg/png，最大5MB）
     * @return 上传结果，包含图片URL
     */
    @PostMapping("/{schoolId}/avatar")
    public ApiResponse<UploadResultDTO> uploadSchoolAvatar(
            @PathVariable Long schoolId,
            @RequestParam("file") MultipartFile file) {
        log.info("uploadSchoolAvatar: schoolId={}, fileName={}", schoolId, file.getOriginalFilename());
        UploadResultDTO result = fileUploadAppService.uploadSchoolAvatar(schoolId, file);
        return ApiResponse.success(result);
    }

    /**
     * 关联用户与学校
     */
    @PostMapping("/{schoolId}/users/add")
    public ApiResponse<UserSchoolAssociationDTO> associateUserWithSchool(
            @PathVariable Long schoolId,
            @Valid @RequestBody AssociateUserWithSchoolCommand command) {
        log.info("associateUserWithSchool: schoolId={}, request={}", schoolId, JSONUtil.toJsonStr(command));
        UserSchoolAssociationDTO association = organizationAppService.associateUserWithSchool(schoolId, command);
        return ApiResponse.success(association);
    }

    /**
     * 获取用户的学校列表
     */
    @GetMapping("/users/{userId}/schools")
    public ApiResponse<List<UserSchoolAssociationDTO>> getUserSchools(@PathVariable Long userId) {
        log.info("getUserSchools: userId={}", userId);
        List<UserSchoolAssociationDTO> schools = organizationAppService.getUserSchools(userId);
        return ApiResponse.success(schools);
    }

    /**
     * 移除用户与学校的关联
     */
    @PostMapping("/{schoolId}/users/{userId}/remove")
    public ApiResponse<Void> removeUserFromSchool(
            @PathVariable Long schoolId,
            @PathVariable Long userId) {
        log.info("removeUserFromSchool: schoolId={}, userId={}", schoolId, userId);
        organizationAppService.removeUserFromSchool(userId, schoolId);
        return ApiResponse.success(null);
    }

    /**
     * 检查用户学校权限
     */
    @GetMapping("/{schoolId}/users/{userId}/check")
    public ApiResponse<Boolean> checkUserPermission(
            @PathVariable Long schoolId,
            @PathVariable Long userId) {
        log.info("checkUserPermission: schoolId={}, userId={}", schoolId, userId);
        boolean hasPermission = organizationAppService.checkUserSchoolPermission(userId, schoolId);
        return ApiResponse.success(hasPermission);
    }
}