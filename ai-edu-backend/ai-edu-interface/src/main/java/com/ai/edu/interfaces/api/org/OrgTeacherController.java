package com.ai.edu.interfaces.api.org;

import cn.hutool.json.JSONUtil;
import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.org.OrgTeacherDTO;
import com.ai.edu.application.dto.org.PageResult;
import com.ai.edu.application.dto.org.command.CreateOrgTeacherCommand;
import com.ai.edu.application.dto.org.command.OrgTeacherQueryParamDTO;
import com.ai.edu.application.dto.org.command.UpdateOrgTeacherCommand;
import com.ai.edu.application.service.org.OrgTeacherAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 教职工管理控制器
 * 教职工本质是用户与部门的关联关系，不存储用户基本信息
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/schools")
@Tag(name = "教职工管理", description = "教职工添加、查询、修改、删除等操作")
public class OrgTeacherController {

    @Resource
    private OrgTeacherAppService orgTeacherAppService;

    /**
     * 添加教职工
     * 提交用户基本信息（姓名、手机号）和所属部门ID
     * 流程：查询用户 → 不存在则创建用户 → 创建组织关联关系
     */
    @Operation(summary = "添加教职工", description = "添加教职工到指定学校的行政部门。如果用户不存在则自动创建用户。")
    @PostMapping("/{schoolId}/addTeacher")
    public ApiResponse<OrgTeacherDTO> addTeacher(
            @Parameter(description = "学校ID") @PathVariable Long schoolId,
            @Valid @RequestBody CreateOrgTeacherCommand command) {
        log.info("添加教职工: schoolId={}, request={}", schoolId, JSONUtil.toJsonStr(command));

        // TODO: 从登录上下文获取当前用户ID
        Long currentUserId = 1L; // 暂时使用固定值

        OrgTeacherDTO teacher = orgTeacherAppService.createOrgTeacher(schoolId, currentUserId, command);
        return ApiResponse.success(teacher);
    }

    /**
     * 查询教职工详情
     * 返回完整信息：userId + departmentId + 用户基本信息（姓名、手机号）
     */
    @Operation(summary = "查询教职工详情", description = "查询指定教职工的详细信息，包括用户基本信息和所属部门信息。")
    @GetMapping("/{schoolId}/getTeacher/{id}")
    public ApiResponse<OrgTeacherDTO> getTeacherDetail(
            @Parameter(description = "学校ID") @PathVariable Long schoolId,
            @Parameter(description = "教职工ID") @PathVariable Long id) {
        log.info("查询教职工详情: schoolId={}, teacherId={}", schoolId, id);

        OrgTeacherDTO teacher = orgTeacherAppService.getOrgTeacher(schoolId, id);
        return ApiResponse.success(teacher);
    }

    /**
     * 查询教职工列表
     * 按学校、按部门查询，返回完整信息
     */
    @Operation(summary = "查询教职工列表", description = "查询指定学校的教职工列表，支持按部门筛选。返回教职工基本信息和所属部门信息。")
    @PostMapping("/{schoolId}/getTeacherList")
    public ApiResponse<PageResult<OrgTeacherDTO>> getTeacherList(
            @Parameter(description = "学校ID") @PathVariable Long schoolId,
            @Valid @RequestBody OrgTeacherQueryParamDTO queryDTO) {
        log.info("查询教职工列表: schoolId={}, departmentId={}, userId={}, pageNum={}, pageSize={}",
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
    @Operation(summary = "更新教职工所属部门", description = "修改教职工的所属行政部门。注意：用户基本信息修改请到用户中心处理。")
    @PostMapping("/{schoolId}/updateTeacher")
    public ApiResponse<OrgTeacherDTO> updateTeacherDepartment(
            @Parameter(description = "学校ID") @PathVariable Long schoolId,
            @Valid @RequestBody UpdateOrgTeacherCommand command) {
        log.info("更新教职工所属部门: schoolId={}, orgTeacherId={}, newDepartmentId={}",
                schoolId, command.getOrgTeacherId(), command.getDepartmentId());

        // TODO: 从登录上下文获取当前用户ID
        Long currentUserId = 1L; // 暂时使用固定值

        OrgTeacherDTO teacher = orgTeacherAppService.updateOrgTeacher(schoolId, currentUserId, command);
        return ApiResponse.success(teacher);
    }

    /**
     * 删除教职工关联关系
     * 只删除组织域的关联关系，用户域的用户数据保留不受影响
     */
    @Operation(summary = "删除教职工", description = "删除教职工与部门的关联关系。注意：用户基本信息保留，只是解除组织关系。")
    @PostMapping("/{schoolId}/deleteTeacher/{id}")
    public ApiResponse<Void> removeTeacher(
            @Parameter(description = "学校ID") @PathVariable Long schoolId,
            @Parameter(description = "教职工ID") @PathVariable Long id) {
        log.info("删除教职工关联关系: schoolId={}, teacherId={}", schoolId, id);

        orgTeacherAppService.deleteOrgTeacher(schoolId, id);
        return ApiResponse.success(null);
    }
}