package com.ai.edu.interfaces.api.learning;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.learning.StudentMasteryDTO;
import com.ai.edu.application.service.learning.TutoringAppService;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生知识点掌握度查询（知识图谱叠加数据源）。
 *
 * <p>身份以会话为准：路径 studentId 必须与会话 userId 一致（服务端不信任客户端传入的身份），
 * 防止越权查询他人掌握度。
 */
@Slf4j
@RestController
@RequestMapping("/api/students")
@Tag(name = "学生掌握度", description = "查询学生知识点掌握度（图谱叠加用）")
public class StudentMasteryController {

    @Resource
    private TutoringAppService tutoringAppService;

    /** GET /api/students/{studentId}/mastery — 图谱前端按 kp_key(URI) 叠加掌握度。 */
    @Operation(summary = "查询学生掌握度", description = "返回该学生全部已记录知识点掌握度，图谱按 kpKey 匹配节点 URI 渲染")
    @GetMapping("/{studentId}/mastery")
    public ApiResponse<StudentMasteryDTO> getMastery(@PathVariable Long studentId, HttpSession session) {
        Long sessionUserId = TutoringAuth.requireStudent(session);
        if (!sessionUserId.equals(studentId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权访问他人掌握度");
        }
        return ApiResponse.success(tutoringAppService.getStudentMastery(sessionUserId));
    }
}
