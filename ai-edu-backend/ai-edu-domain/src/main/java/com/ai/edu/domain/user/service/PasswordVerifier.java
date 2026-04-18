package com.ai.edu.domain.user.service;

/**
 * 密码验证器 - 领域层接口
 * 由基础设施层/应用层提供 bcrypt 实现
 */
@FunctionalInterface
public interface PasswordVerifier {
    boolean matches(String rawPassword, String encodedPassword);
}
