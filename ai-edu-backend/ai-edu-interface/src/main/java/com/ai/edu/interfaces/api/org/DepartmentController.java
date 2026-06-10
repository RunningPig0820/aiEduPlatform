package com.ai.edu.interfaces.api.org;

import cn.hutool.json.JSONUtil;
import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.org.DepartmentDTO;
import com.ai.edu.application.dto.org.PageResult;
import com.ai.edu.application.dto.org.command.CreateDepartmentCommand;
import com.ai.edu.application.dto.org.command.DepartmentQueryCommand;
import com.ai.edu.application.dto.org.command.UpdateDepartmentCommand;
import com.ai.edu.application.service.org.DepartmentAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/schools/{schoolId}/departments")
public class DepartmentController {

    @Resource
    private DepartmentAppService departmentAppService;

    /**
     * 创建部门
     */
    @PostMapping("/create")
    public ApiResponse<DepartmentDTO> createDepartment(
            @PathVariable Long schoolId,
            @Valid @RequestBody CreateDepartmentCommand command) {
        log.info("createDepartment: schoolId={}, request={}", schoolId, JSONUtil.toJsonStr(command));
        DepartmentDTO department = departmentAppService.createDepartment(schoolId, command);
        return ApiResponse.success(department);
    }

    /**
     * 更新部门
     */
    @PostMapping("/{id}/update")
    public ApiResponse<DepartmentDTO> updateDepartment(
            @PathVariable Long schoolId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentCommand command) {
        log.info("updateDepartment: schoolId={}, id={}, request={}", schoolId, id, JSONUtil.toJsonStr(command));
        DepartmentDTO department = departmentAppService.updateDepartment(schoolId, id, command);
        return ApiResponse.success(department);
    }

    /**
     * 删除部门
     */
    @PostMapping("/{id}/delete")
    public ApiResponse<Void> deleteDepartment(
            @PathVariable Long schoolId,
            @PathVariable Long id) {
        log.info("deleteDepartment: schoolId={}, id={}", schoolId, id);
        departmentAppService.deleteDepartment(schoolId, id);
        return ApiResponse.success(null);
    }

    /**
     * 获取部门详情
     */
    @GetMapping("/{id}")
    public ApiResponse<DepartmentDTO> getDepartment(
            @PathVariable Long schoolId,
            @PathVariable Long id) {
        log.info("getDepartment: schoolId={}, id={}", schoolId, id);
        DepartmentDTO department = departmentAppService.getDepartmentById(schoolId, id);
        return ApiResponse.success(department);
    }

    /**
     * 获取部门树
     */
    @GetMapping
    public ApiResponse<List<DepartmentDTO>> getDepartmentTree(@PathVariable Long schoolId) {
        log.info("getDepartmentTree: schoolId={}", schoolId);
        List<DepartmentDTO> tree = departmentAppService.getDepartmentTree(schoolId);
        return ApiResponse.success(tree);
    }

    /**
     * 分页查询部门
     */
    @PostMapping("/page")
    public ApiResponse<PageResult<DepartmentDTO>> pageDepartments(
            @PathVariable Long schoolId,
            @RequestBody DepartmentQueryCommand query) {
        log.info("pageDepartments: schoolId={}, id={}, name={}, pageNum={}, pageSize={}",
                schoolId, query.getId(), query.getName(), query.getPageNum(), query.getPageSize());
        PageResult<DepartmentDTO> result = departmentAppService.queryDepartments(schoolId, query);
        return ApiResponse.success(result);
    }
}