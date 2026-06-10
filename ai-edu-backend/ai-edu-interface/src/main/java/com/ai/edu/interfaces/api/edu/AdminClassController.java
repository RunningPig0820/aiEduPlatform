package com.ai.edu.interfaces.api.edu;

import cn.hutool.json.JSONUtil;
import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.org.AdminClassNodeDTO;
import com.ai.edu.application.dto.org.GradeOptionDTO;
import com.ai.edu.application.dto.org.StageConfigDTO;
import com.ai.edu.application.dto.org.command.CreateAdminClassNodeCommand;
import com.ai.edu.application.dto.org.command.UpdateAdminClassNodeCommand;
import com.ai.edu.application.service.org.AdminClassAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 行政班 API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/admin-classes")
public class AdminClassController {

    @Resource
    private AdminClassAppService adminClassAppService;

    /**
     * 创建行政班节点
     */
    @PostMapping
    public ApiResponse<AdminClassNodeDTO> createNode(@Valid @RequestBody CreateAdminClassNodeCommand command) {
        log.info("createAdminClassNode: request={}", JSONUtil.toJsonStr(command));
        AdminClassNodeDTO node = adminClassAppService.createNode(command);
        return ApiResponse.success(node);
    }

    /**
     * 更新行政班节点
     */
    @PutMapping("/{id}")
    public ApiResponse<AdminClassNodeDTO> updateNode(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAdminClassNodeCommand command) {
        log.info("updateAdminClassNode: id={}, request={}", id, JSONUtil.toJsonStr(command));
        AdminClassNodeDTO node = adminClassAppService.updateNode(id, command);
        return ApiResponse.success(node);
    }

    /**
     * 获取行政班节点详情
     */
    @GetMapping("/{id}")
    public ApiResponse<AdminClassNodeDTO> getNodeDetail(@PathVariable Long id) {
        log.info("getAdminClassNodeDetail: id={}", id);
        AdminClassNodeDTO node = adminClassAppService.getNodeDetail(id);
        return ApiResponse.success(node);
    }

    /**
     * 获取学校行政班树
     */
    @GetMapping
    public ApiResponse<List<AdminClassNodeDTO>> getNodeTree(@RequestParam Long schoolId) {
        log.info("getAdminClassTree: schoolId={}", schoolId);
        List<AdminClassNodeDTO> tree = adminClassAppService.getNodeTree(schoolId);
        return ApiResponse.success(tree);
    }

    /**
     * 获取学段配置列表（固定选项：学段/年制/可用年级）
     */
    @GetMapping("/stage-configs")
    public ApiResponse<List<StageConfigDTO>> getStageConfigs() {
        log.info("getStageConfigs");
        List<StageConfigDTO> configs = adminClassAppService.getStageConfigs();
        return ApiResponse.success(configs);
    }

    /**
     * 根据学段和年制获取年级选项
     */
    @GetMapping("/grade-options")
    public ApiResponse<List<GradeOptionDTO>> getGradeOptions(
            @RequestParam String stageCode,
            @RequestParam String stageYearCode) {
        log.info("getGradeOptions: stageCode={}, stageYearCode={}", stageCode, stageYearCode);
        List<GradeOptionDTO> grades = adminClassAppService.getGradeOptions(stageCode, stageYearCode);
        return ApiResponse.success(grades);
    }

    /**
     * 删除行政班节点
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNode(@PathVariable Long id) {
        log.info("deleteAdminClassNode: id={}", id);
        adminClassAppService.deleteNode(id);
        return ApiResponse.success(null);
    }
}
