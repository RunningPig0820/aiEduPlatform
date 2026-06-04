package com.ai.edu.interfaces.security;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.service.org.OrganizationAppService;
import com.ai.edu.common.constant.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

/**
 * 学校权限拦截器
 * 拦截标注了 @SchoolScoped 的请求，校验用户是否关联目标学校
 */
@Slf4j
@Component
public class SchoolPermissionInterceptor implements HandlerInterceptor {

    @Resource
    private OrganizationAppService organizationAppService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 只处理 Controller 方法
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 检查方法是否标注了 @SchoolScoped
        SchoolScoped annotation = handlerMethod.getMethodAnnotation(SchoolScoped.class);
        if (annotation == null) {
            return true;
        }

        log.info("SchoolPermissionInterceptor: 校验学校权限, uri={}", request.getRequestURI());

        // 1. 从 Session 获取用户 ID
        Long userId = (Long) request.getSession().getAttribute("userId");
        if (userId == null) {
            log.warn("SchoolPermissionInterceptor: 用户未登录");
            writeErrorResponse(response, ErrorCode.UNAUTHORIZED, "未登录");
            return false;
        }

        // 2. 从 URL 提取学校 ID
        Long schoolId = extractSchoolId(request, annotation.schoolIdParam());
        if (schoolId == null) {
            log.warn("SchoolPermissionInterceptor: 无法提取 schoolId");
            writeErrorResponse(response, ErrorCode.PARAM_ERROR, "缺少 schoolId 参数");
            return false;
        }

        // 3. 查询用户在学校中的角色
        String role = organizationAppService.getUserRoleInSchool(userId, schoolId);
        if (role == null) {
            log.warn("SchoolPermissionInterceptor: 用户未关联学校, userId={}, schoolId={}", userId, schoolId);
            writeErrorResponse(response, ErrorCode.NO_SCHOOL_ACCESS, "无学校访问权限");
            return false;
        }

        // 4. 如果要求管理员角色，校验角色
        if (annotation.requireAdmin() && !"ADMIN".equals(role)) {
            log.warn("SchoolPermissionInterceptor: 用户非管理员, userId={}, schoolId={}, role={}", userId, schoolId, role);
            writeErrorResponse(response, ErrorCode.SCHOOL_USER_NOT_ADMIN, "需要管理员权限");
            return false;
        }

        // 5. 设置学校上下文
        SchoolContextHolder.setContext(schoolId, userId, role);
        log.info("SchoolPermissionInterceptor: 权限校验通过, userId={}, schoolId={}, role={}", userId, schoolId, role);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清除上下文，防止内存泄漏
        SchoolContextHolder.clear();
    }

    /**
     * 从 URL 提取学校 ID
     */
    private Long extractSchoolId(HttpServletRequest request, String paramName) {
        // 1. 尝试从路径参数提取
        String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            String[] segments = pathInfo.split("/");
            for (int i = 0; i < segments.length - 1; i++) {
                if (segments[i].equals("schools") && i + 1 < segments.length) {
                    try {
                        return Long.parseLong(segments[i + 1]);
                    } catch (NumberFormatException e) {
                        // 继续尝试其他方式
                    }
                }
            }
        }

        // 2. 从 URI 提取
        String uri = request.getRequestURI();
        String[] uriSegments = uri.split("/");
        for (int i = 0; i < uriSegments.length - 1; i++) {
            if (uriSegments[i].equals("schools") && i + 1 < uriSegments.length) {
                try {
                    return Long.parseLong(uriSegments[i + 1]);
                } catch (NumberFormatException e) {
                    // 继续尝试其他方式
                }
            }
        }

        // 3. 从请求参数提取
        String schoolIdStr = request.getParameter(paramName);
        if (schoolIdStr != null) {
            try {
                return Long.parseLong(schoolIdStr);
            } catch (NumberFormatException e) {
                log.warn("无法解析 schoolId 参数: {}", schoolIdStr);
            }
        }

        // 4. 从路径变量提取（Spring MVC 的 @PathVariable）
        Enumeration<String> attrNames = request.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String attrName = attrNames.nextElement();
            if (attrName.contains(paramName) || attrName.equals(paramName)) {
                Object attrValue = request.getAttribute(attrName);
                if (attrValue instanceof Long) {
                    return (Long) attrValue;
                }
                if (attrValue instanceof String) {
                    try {
                        return Long.parseLong((String) attrValue);
                    } catch (NumberFormatException e) {
                        // 继续
                    }
                }
            }
        }

        return null;
    }

    /**
     * 写入错误响应
     */
    private void writeErrorResponse(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(code, message)));
    }
}