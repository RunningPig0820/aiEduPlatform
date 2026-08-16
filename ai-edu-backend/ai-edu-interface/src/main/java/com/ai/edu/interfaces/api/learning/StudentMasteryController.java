package com.ai.edu.interfaces.api.learning;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.learning.KpCoverageDTO;
import com.ai.edu.application.dto.learning.PendingKpAliasDTO;
import com.ai.edu.application.dto.learning.StudentMasteryDTO;
import com.ai.edu.application.service.learning.KpAppService;
import com.ai.edu.application.service.learning.KpCoverageAppService;
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

import java.util.List;

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

    @Resource
    private KpAppService kpAppService;

    @Resource
    private KpCoverageAppService kpCoverageAppService;

    /** GET /api/students/{studentId}/mastery — 题型掌握度（掌握度主体翻转：题型粒度）。 */
    @Operation(summary = "查询学生题型掌握度", description = "返回该学生全部题型掌握度（topicKey/topicLabel/masteryLevel/status/confidence）")
    @GetMapping("/{studentId}/mastery")
    public ApiResponse<StudentMasteryDTO> getMastery(@PathVariable Long studentId, HttpSession session) {
        Long sessionUserId = TutoringAuth.requireStudent(session);
        if (!sessionUserId.equals(studentId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权访问他人掌握度");
        }
        return ApiResponse.success(tutoringAppService.getStudentMastery(sessionUserId));
    }

    /** GET /api/students/{studentId}/kp-coverage — 知识点派生覆盖度（知识点总览知识地图着色）。 */
    @Operation(summary = "查询学生知识点派生覆盖度", description = "返回该学生知识点派生覆盖度（coverage 连续值 + masteryLevel 离散四档 + 学段归属）")
    @GetMapping("/{studentId}/kp-coverage")
    public ApiResponse<KpCoverageDTO> getKpCoverage(@PathVariable Long studentId, HttpSession session) {
        Long sessionUserId = TutoringAuth.requireStudent(session);
        if (!sessionUserId.equals(studentId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权访问他人数据");
        }
        return ApiResponse.success(kpCoverageAppService.getKpCoverage(sessionUserId));
    }

    /** GET /api/students/{studentId}/pending-kps — 学生端"待确认清单"（疑似薄弱点）数据源。 */
    @Operation(summary = "查询学生疑似观测", description = "返回该学生 PENDING/WEAK 派生观测（待确认清单）")
    @GetMapping("/{studentId}/pending-kps")
    public ApiResponse<List<PendingKpAliasDTO>> getPendingKps(@PathVariable Long studentId, HttpSession session) {
        Long sessionUserId = TutoringAuth.requireStudent(session);
        if (!sessionUserId.equals(studentId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权访问他人数据");
        }
        return ApiResponse.success(kpAppService.listPendingByStudent(sessionUserId));
    }
}
