package com.ai.edu.interfaces.api;

import cn.hutool.json.JSONUtil;
import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.org.OrgTeacherDTO;
import com.ai.edu.application.dto.org.PageResult;
import com.ai.edu.application.dto.org.command.CreateOrgTeacherCommand;
import com.ai.edu.application.dto.org.command.OrgTeacherQueryParamDTO;
import com.ai.edu.application.dto.org.command.UpdateOrgTeacherCommand;
import com.ai.edu.application.service.org.OrgTeacherAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 教职工API控制器
 * 教职工本质是用户与部门的关联关系，不存储用户基本信息
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/schools/{schoolId}/teachers")
public class OrgTeacherController {

    @Resource
    private OrgTeacherAppService orgTeacherAppService;

    /**
     * 创建教职工（关联关系）
     * 提交用户基本信息（姓名、手机号）和所属部门ID
     * 流程：查询用户 → 不存在则创建用户 → 创建组织关联关系
     */
    @PostMapping
    public ApiResponse<OrgTeacherDTO> createOrgTeacher(
            @PathVariable Long schoolId,
            @Valid @RequestBody CreateOrgTeacherCommand command) {
        log.info("createOrgTeacher: schoolId={}, request={}", schoolId, JSONUtil.toJsonStr(command));

        // TODO: 从登录上下文获取当前用户ID
        Long currentUserId = 1L; // 暂时使用固定值

        OrgTeacherDTO teacher = orgTeacherAppService.createOrgTeacher(schoolId, currentUserId, command);
        return ApiResponse.success(teacher);
    }

    /**
     * 查询教职工详情（聚合查询）
     * 返回完整信息：userId + departmentId + 用户基本信息（姓名、手机号）
     */
    @GetMapping("/{id}")
    public ApiResponse<OrgTeacherDTO> getOrgTeacher(
            @PathVariable Long schoolId,
            @PathVariable Long id) {
        log.info("getOrgTeacher: schoolId={}, id={}", schoolId, id);

        OrgTeacherDTO teacher = orgTeacherAppService.getOrgTeacher(schoolId, id);
        return ApiResponse.success(teacher);
    }

    /**
     * 查询教职工列表（聚合查询）
     * 按学校、按部门查询，返回完整信息
     */
    @GetMapping
    public ApiResponse<PageResult<OrgTeacherDTO>> listOrgTeachers(
            @PathVariable Long schoolId,
            OrgTeacherQueryParamDTO queryDTO) {
        log.info("listOrgTeachers: schoolId={}, departmentId={}, userId={}, pageNum={}, pageSize={}",
                schoolId, queryDTO.getDepartmentId(), queryDTO.getUserId(),
                queryDTO.getPageNum(), queryDTO.getPageSize());

        // 设置默认分页参数
        if (queryDTO.getPageNum() == null) {
            queryDTO.setPageNum(1);
        }
        if (queryDTO.getPageSize() == null) {
            queryDTO.setPageSize(10);
        }

        PageResult<OrgTeacherDTO> result = orgTeacherAppService.listOrgTeachers(schoolId, queryDTO);
        return ApiResponse.success(result);
    }

    /**
     * 更新教职工所属部门
     * 组织域只支持修改所属部门，用户基本信息修改在用户中心处理
     */
    @PutMapping("/{id}")
    public ApiResponse<OrgTeacherDTO> updateOrgTeacher(
            @PathVariable Long schoolId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrgTeacherCommand command) {
        log.info("updateOrgTeacher: schoolId={}, id={}, request={}", schoolId, id, JSONUtil.toJsonStr(command));

        // TODO: 从登录上下文获取当前用户ID
        Long currentUserId = 1L; // 暂时使用固定值

        OrgTeacherDTO teacher = orgTeacherAppService.updateOrgTeacher(schoolId, id, currentUserId, command);
        return ApiResponse.success(teacher);
    }

    /**
     * 删除教职工关联关系
     * 只删除组织域的关联关系，用户域的用户数据保留不受影响
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteOrgTeacher(
            @PathVariable Long schoolId,
            @PathVariable Long id) {
        log.info("deleteOrgTeacher: schoolId={}, id={}", schoolId, id);

        orgTeacherAppService.deleteOrgTeacher(schoolId, id);
        return ApiResponse.success(null);
    }
}