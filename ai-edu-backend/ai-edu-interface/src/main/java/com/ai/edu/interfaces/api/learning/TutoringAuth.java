package com.ai.edu.interfaces.api.learning;

import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import jakarta.servlet.http.HttpSession;

/**
 * 答疑接口认证辅助——登录态从 HttpSession 取（userId 即 studentId），校验 STUDENT 角色。
 *
 * <p>沿用 {@code LlmApiController} 的 session 写法；角色从登录时写入的 {@code role} 属性读取。
 */
final class TutoringAuth {

    private TutoringAuth() {
    }

    /** 从会话取学生 ID（userId 即 studentId）；未登录返回 null。 */
    static Long currentStudentId(HttpSession session) {
        Object userId = session.getAttribute("userId");
        return userId instanceof Long ? (Long) userId : null;
    }

    /** 会话角色是否为 STUDENT。 */
    static boolean isStudent(HttpSession session) {
        return "STUDENT".equals(session.getAttribute("role"));
    }

    /** 取学生 ID 并校验登录 + STUDENT 角色；失败抛业务异常（同步端点用）。 */
    static Long requireStudent(HttpSession session) {
        Long studentId = currentStudentId(session);
        if (studentId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!isStudent(session)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "仅学生可访问");
        }
        return studentId;
    }
}
