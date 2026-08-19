package com.ai.edu.interfaces.api.learning;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.learning.MasteryQueryRequest;
import com.ai.edu.application.dto.learning.PendingKpAliasDTO;
import com.ai.edu.application.dto.learning.StudentMasteryDTO;
import com.ai.edu.application.dto.learning.StudentTopicQuestionsDTO;
import com.ai.edu.application.service.learning.KpAppService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学生题型掌握度/题目证据查询（域 B 独立化：掌握度主体 = 题型；知识点覆盖率按需由「题型掌握度 × 题型↔知识点映射」派生）。
 *
 * <p>身份以会话为准：路径 studentId 必须与会话 userId 一致（服务端不信任客户端传入的身份），
 * 防止越权查询他人掌握度。
 */
@Slf4j
@RestController
@RequestMapping("/api/students")
@Tag(name = "学生掌握度", description = "题型掌握度分页查询 + 按题型查题目证据 + 疑似观测")
public class StudentMasteryController {

    @Resource
    private TutoringAppService tutoringAppService;

    @Resource
    private KpAppService kpAppService;

    /** POST /api/students/{studentId}/mastery/query — 题型掌握度分页查询（4.1 分页改造：分页/status 分桶/keyword 模糊/排序）。 */
    @Operation(summary = "查询学生题型掌握度（分页）", description = "分页返回该学生已归属题型掌握度（默认 updatedAt 倒序；masteryStatus 分桶筛选；keyword 模糊；sortBy 切 masteryLevel）")
    @PostMapping("/{studentId}/mastery/query")
    public ApiResponse<StudentMasteryDTO> queryMastery(@PathVariable Long studentId,
                                                       @RequestBody(required = false) MasteryQueryRequest request,
                                                       HttpSession session) {
        Long sessionUserId = TutoringAuth.requireStudent(session);
        if (!sessionUserId.equals(studentId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权访问他人掌握度");
        }
        return ApiResponse.success(tutoringAppService.queryStudentMastery(sessionUserId, request));
    }

    /** GET /api/students/{studentId}/topics/{topicLabel}/questions — 按题型查题目（掌握度页「查看题目」，tasks 4.2）。 */
    @Operation(summary = "按题型查题目", description = "该题型全部证据题（content/score/session_id 原题链接），空列表不报错")
    @GetMapping("/{studentId}/topics/{topicLabel}/questions")
    public ApiResponse<StudentTopicQuestionsDTO> getTopicQuestions(@PathVariable Long studentId,
                                                                   @PathVariable String topicLabel,
                                                                   HttpSession session) {
        Long sessionUserId = TutoringAuth.requireStudent(session);
        if (!sessionUserId.equals(studentId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权访问他人数据");
        }
        return ApiResponse.success(tutoringAppService.getStudentTopicQuestions(sessionUserId, topicLabel));
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
