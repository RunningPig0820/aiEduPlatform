package com.ai.edu.interfaces.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 学校权限校验注解
 * 标注在 Controller 方法上，表示该接口需要校验用户是否关联目标学校
 *
 * 使用示例:
 * <pre>
 * @SchoolScoped
 * @GetMapping("/api/schools/{schoolId}/classes")
 * public ApiResponse<List<Class>> getSchoolClasses(@PathVariable Long schoolId) {
 *     // 只有关联了该学校的用户才能访问
 * }
 * </pre>
 *
 * 拦截器会从 URL 中提取 schoolId 参数，并校验当前用户是否关联该学校
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SchoolScoped {

    /**
     * URL 中学校 ID 的参数名
     * 默认为 "schoolId"
     */
    String schoolIdParam() default "schoolId";

    /**
     * 是否要求管理员角色
     * 如果为 true，则用户必须在该学校中具有 ADMIN 角色
     */
    boolean requireAdmin() default false;
}