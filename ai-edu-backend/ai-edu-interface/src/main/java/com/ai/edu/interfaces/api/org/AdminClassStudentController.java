package com.ai.edu.interfaces.api.org;

import cn.hutool.json.JSONUtil;
import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.org.AdminClassStudentDTO;
import com.ai.edu.application.dto.org.command.CreateAdminClassStudentCommand;
import com.ai.edu.application.dto.org.command.UpdateAdminClassStudentCommand;
import com.ai.edu.application.service.org.AdminClassStudentAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 行政班学生管理控制器
 *
 * 将学生添加到行政班树的班级节点（Department with DeptEduType=CLASS）。
 * 跨域编排：创建/查询学生用户 + 家长用户 + StudentClass 关联 + ParentProfile 绑定。
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/schools/{schoolId}/admin-classes")
@Tag(name = "行政班学生管理", description = "行政班学生的添加、查询等操作")
public class AdminClassStudentController {

    @Resource
    private AdminClassStudentAppService adminClassStudentAppService;

    /**
     * 添加学生到行政班班级节点
     *
     * 提交学生基本信息（姓名、手机号、身份证号、学号）和家长列表。
     * 流程：
     * 1. 验证行政班班级节点
     * 2. 创建/查询学生用户（含身份证 AES 加密）
     * 3. 创建/查询家长用户（手机号未注册则自动注册）
     * 4. 创建 StudentClass 关联
     * 5. 绑定家长关联 ParentProfile
     */
    @Operation(summary = "添加学生到行政班", description = "将学生添加到指定行政班班级节点。学生不存在则自动创建用户（含身份证加密），家长手机号未注册则自动创建家长用户。")
    @PostMapping("/{deptId}/students")
    public ApiResponse<AdminClassStudentDTO> addStudent(
            @Parameter(description = "学校ID") @PathVariable Long schoolId,
            @Parameter(description = "行政班班级节点ID（Department.id）") @PathVariable Long deptId,
            @Valid @RequestBody CreateAdminClassStudentCommand command) {
        log.info("添加行政班学生: schoolId={}, deptId={}, request={}",
                schoolId, deptId, JSONUtil.toJsonStr(command));

        // TODO: 从登录上下文获取当前用户ID
        Long currentUserId = 1L; // 暂时使用固定值

        AdminClassStudentDTO student = adminClassStudentAppService.createStudent(
                schoolId, deptId, currentUserId, command);
        return ApiResponse.success(student);
    }

    /**
     * 查询行政班班级下的学生列表
     *
     * 聚合返回：StudentClass 关联 + 学生用户基本信息（含脱敏身份证）+ 绑定家长列表
     */
    @Operation(summary = "查询行政班学生列表", description = "查询指定行政班班级节点下的所有在读学生，聚合返回学生基本信息和家长信息。")
    @GetMapping("/{deptId}/students")
    public ApiResponse<List<AdminClassStudentDTO>> listStudents(
            @Parameter(description = "学校ID") @PathVariable Long schoolId,
            @Parameter(description = "行政班班级节点ID（Department.id）") @PathVariable Long deptId) {
        log.info("查询行政班学生列表: schoolId={}, deptId={}", schoolId, deptId);

        List<AdminClassStudentDTO> students = adminClassStudentAppService.listStudentsByClass(deptId, schoolId);
        return ApiResponse.success(students);
    }

    /**
     * 修改行政班学生信息
     *
     * 组织域只支持修改学号和状态（毕业/转出/恢复在读）。
     * 用户基本信息修改请到用户中心处理。
     */
    @Operation(summary = "修改行政班学生", description = "修改学生的学号或状态（毕业/转出/恢复在读）。用户基本信息修改请到用户中心。")
    @PutMapping("/{deptId}/students")
    public ApiResponse<AdminClassStudentDTO> updateStudent(
            @Parameter(description = "学校ID") @PathVariable Long schoolId,
            @Parameter(description = "行政班班级节点ID") @PathVariable Long deptId,
            @RequestBody UpdateAdminClassStudentCommand command) {
        log.info("修改行政班学生: schoolId={}, deptId={}, command={}", schoolId, deptId, JSONUtil.toJsonStr(command));

        Long currentUserId = 1L;
        AdminClassStudentDTO student = adminClassStudentAppService.updateStudent(schoolId, currentUserId, command);
        return ApiResponse.success(student);
    }

    /**
     * 删除行政班学生关联
     *
     * 只删除组织域的关联关系，用户域的学生和家长用户数据保留不受影响。
     */
    @Operation(summary = "删除行政班学生", description = "删除学生与行政班的关联关系。注意：学生和家长用户数据保留，只解除组织关系。")
    @DeleteMapping("/{deptId}/students/{id}")
    public ApiResponse<Void> deleteStudent(
            @Parameter(description = "学校ID") @PathVariable Long schoolId,
            @Parameter(description = "行政班班级节点ID") @PathVariable Long deptId,
            @Parameter(description = "StudentClass 关联ID") @PathVariable Long id) {
        log.info("删除行政班学生关联: schoolId={}, deptId={}, id={}", schoolId, deptId, id);

        adminClassStudentAppService.deleteStudent(schoolId, id);
        return ApiResponse.success(null);
    }
}
