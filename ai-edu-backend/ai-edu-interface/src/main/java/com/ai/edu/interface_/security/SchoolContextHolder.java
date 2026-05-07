package com.ai.edu.interface_.security;

import lombok.Getter;
import lombok.Setter;

/**
 * 学校上下文持有者
 * 在请求范围内存储当前用户的学校信息
 *
 * 使用 ThreadLocal 确保线程安全
 */
public class SchoolContextHolder {

    private static final ThreadLocal<SchoolContext> CONTEXT = new ThreadLocal<>();

    /**
     * 设置学校上下文
     */
    public static void setContext(Long schoolId, Long userId, String role) {
        CONTEXT.set(new SchoolContext(schoolId, userId, role));
    }

    /**
     * 获取学校上下文
     */
    public static SchoolContext getContext() {
        return CONTEXT.get();
    }

    /**
     * 获取当前学校ID
     */
    public static Long getSchoolId() {
        SchoolContext context = CONTEXT.get();
        return context != null ? context.getSchoolId() : null;
    }

    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        SchoolContext context = CONTEXT.get();
        return context != null ? context.getUserId() : null;
    }

    /**
     * 获取当前用户在学校中的角色
     */
    public static String getRole() {
        SchoolContext context = CONTEXT.get();
        return context != null ? context.getRole() : null;
    }

    /**
     * 是否是管理员
     */
    public static boolean isAdmin() {
        String role = getRole();
        return role != null && "ADMIN".equals(role);
    }

    /**
     * 清除上下文（请求结束时调用）
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 学校上下文信息
     */
    @Getter
    @Setter
    public static class SchoolContext {
        private Long schoolId;
        private Long userId;
        private String role;

        public SchoolContext(Long schoolId, Long userId, String role) {
            this.schoolId = schoolId;
            this.userId = userId;
            this.role = role;
        }
    }
}