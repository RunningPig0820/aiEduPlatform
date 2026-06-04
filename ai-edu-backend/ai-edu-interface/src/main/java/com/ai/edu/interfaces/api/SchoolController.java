package com.ai.edu.interfaces.api;

import cn.hutool.json.JSONUtil;
import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.file.UploadResultDTO;
import com.ai.edu.application.dto.org.PageResult;
import com.ai.edu.application.dto.org.command.CreateSchoolCommand;
import com.ai.edu.application.dto.org.SchoolDTO;
import com.ai.edu.application.dto.org.command.SchoolQueryCommand;
import com.ai.edu.application.dto.org.command.UpdateSchoolCommand;
import com.ai.edu.application.service.file.FileUploadAppService;
import com.ai.edu.application.service.org.SchoolAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
     * 分页查询学校列表
     */
    @PostMapping("/page")
    public ApiResponse<PageResult<SchoolDTO>> pageSchools(@RequestBody SchoolQueryCommand query) {
        log.info("listSchools: id={}, name={}, type={}, pageNum={}, pageSize={}",
                query.getId(), query.getName(), query.getType(), query.getPageNum(), query.getPageSize());

        PageResult<SchoolDTO> result = schoolAppService.querySchools(query);
        return ApiResponse.success(result);
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
}