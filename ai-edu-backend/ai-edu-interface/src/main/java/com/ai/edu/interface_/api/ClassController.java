package com.ai.edu.interface_.api;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.org.ClassDTO;
import com.ai.edu.application.dto.org.command.AddStudentCommand;
import com.ai.edu.application.dto.org.command.CreateClassCommand;
import com.ai.edu.application.service.org.ClassAppService;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 班级Controller
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class ClassController {

    @Resource
    private ClassAppService classAppService;

    /**
     * 创建班级
     */
    @PostMapping("/classes/create")
    public ApiResponse<ClassDTO> createClass(@Valid @RequestBody CreateClassCommand command) {
        log.info("createClass: request={}", JSONUtil.toJsonStr(command));
        ClassDTO classDTO = classAppService.createClass(command);
        return ApiResponse.success(classDTO);
    }

    /**
     * 获取班级详情
     */
    @GetMapping("/classes/{id}")
    public ApiResponse<ClassDTO> getClass(@PathVariable Long id) {
        log.info("getClass: id={}", id);
        ClassDTO classDTO = classAppService.getClassById(id);
        return ApiResponse.success(classDTO);
    }

    /**
     * 获取学校的班级列表
     */
    @GetMapping("/schools/{schoolId}/classes")
    public ApiResponse<List<ClassDTO>> listClassesBySchool(@PathVariable Long schoolId) {
        log.info("listClassesBySchool: schoolId={}", schoolId);
        List<ClassDTO> classes = classAppService.listClassesBySchool(schoolId);
        return ApiResponse.success(classes);
    }

    /**
     * 添加学生到班级
     */
    @PostMapping("/classes/{classId}/students/add")
    public ApiResponse<Void> addStudent(
            @PathVariable Long classId,
            @Valid @RequestBody AddStudentCommand command) {
        log.info("addStudent: classId={}, studentId={}", classId, command.getStudentId());
        classAppService.addStudent(classId, command);
        return ApiResponse.success(null);
    }

    /**
     * 班级毕业
     */
    @PostMapping("/classes/{id}/graduate")
    public ApiResponse<Void> graduateClass(@PathVariable Long id) {
        log.info("graduateClass: id={}", id);
        classAppService.graduateClass(id);
        return ApiResponse.success(null);
    }
}