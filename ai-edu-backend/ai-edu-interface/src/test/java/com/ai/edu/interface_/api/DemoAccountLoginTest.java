package com.ai.edu.interface_.api;

import com.ai.edu.application.dto.LoginRequest;
import com.ai.edu.application.dto.UserResponse;
import com.ai.edu.application.service.user.UserAppService;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.constant.LoginType;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.common.util.PasswordUtil;
import com.ai.edu.domain.user.model.entity.User;
import com.ai.edu.domain.user.repository.UserRepository;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 演示账号登录测试
 * 测试 student/teacher/parent/admin 四个账号的登录情况
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DemoAccountLoginTest {

    @Resource
    private UserAppService userAppService;

    @Resource
    private UserRepository userRepository;

    private static final String DEFAULT_PASSWORD = "123456";

    // ==================== 初始化测试数据 ====================

    /**
     * 初始化四个演示账号（提交到数据库，不受事务回滚影响）
     */
    @Test
    @Order(0)
    @Transactional
    @Commit  // 确保数据提交
    void initDemoAccounts() {
        createDemoUserIfNotExists("admin", "系统管理员", "ADMIN", "13800138000");
        createDemoUserIfNotExists("student", "演示学生", "STUDENT", "13800138001");
        createDemoUserIfNotExists("teacher", "演示老师", "TEACHER", "13800138002");
        createDemoUserIfNotExists("parent", "演示家长", "PARENT", "13800138003");

        System.out.println("[INIT] 演示账号初始化完成");
    }

    private void createDemoUserIfNotExists(String username, String realName, String role, String phone) {
        if (!userRepository.existsByUsername(username)) {
            String encodedPassword = PasswordUtil.encode(DEFAULT_PASSWORD);
            User user = User.create(username, encodedPassword, realName, phone, role);
            userRepository.save(user);
        }
    }

    // ==================== Admin 登录测试 ====================

    /**
     * 测试 Admin 账号登录 - 用户名密码方式
     */
    @Test
    @Order(1)
    void testAdminLogin_UsernamePassword() {
        UserResponse response = performLogin("admin", DEFAULT_PASSWORD);

        assertEquals("admin", response.getUsername());
        assertEquals("ADMIN", response.getRole());
        assertEquals("系统管理员", response.getRealName());
        assertTrue(response.getEnabled());

        System.out.println("[SUCCESS] Admin 登录成功: username=" + response.getUsername() + ", role=" + response.getRole());
    }

    /**
     * 测试 Admin 账号登录 - 手机号密码方式
     */
    @Test
    @Order(2)
    void testAdminLogin_PhonePassword() {
        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginType.PHONE_PASSWORD);
        request.setPhone("13800138000");
        request.setPassword(DEFAULT_PASSWORD);

        UserResponse response = userAppService.login(request);

        assertEquals("admin", response.getUsername());
        assertEquals("ADMIN", response.getRole());

        System.out.println("[SUCCESS] Admin 手机号登录成功: phone=" + response.getPhone());
    }

    // ==================== Student 登录测试 ====================

    /**
     * 测试 Student 账号登录
     */
    @Test
    @Order(10)
    void testStudentLogin() {
        UserResponse response = performLogin("student", DEFAULT_PASSWORD);

        assertEquals("student", response.getUsername());
        assertEquals("STUDENT", response.getRole());
        assertEquals("演示学生", response.getRealName());
        assertTrue(response.getEnabled());

        System.out.println("[SUCCESS] Student 登录成功: username=" + response.getUsername() + ", role=" + response.getRole());
    }

    // ==================== Teacher 登录测试 ====================

    /**
     * 测试 Teacher 账号登录
     */
    @Test
    @Order(20)
    void testTeacherLogin() {
        UserResponse response = performLogin("teacher", DEFAULT_PASSWORD);

        assertEquals("teacher", response.getUsername());
        assertEquals("TEACHER", response.getRole());
        assertEquals("演示老师", response.getRealName());
        assertTrue(response.getEnabled());

        System.out.println("[SUCCESS] Teacher 登录成功: username=" + response.getUsername() + ", role=" + response.getRole());
    }

    // ==================== Parent 登录测试 ====================

    /**
     * 测试 Parent 账号登录
     */
    @Test
    @Order(30)
    void testParentLogin() {
        UserResponse response = performLogin("parent", DEFAULT_PASSWORD);

        assertEquals("parent", response.getUsername());
        assertEquals("PARENT", response.getRole());
        assertEquals("演示家长", response.getRealName());
        assertTrue(response.getEnabled());

        System.out.println("[SUCCESS] Parent 登录成功: username=" + response.getUsername() + ", role=" + response.getRole());
    }

    // ==================== 登录失败测试 ====================

    /**
     * 测试错误密码登录失败
     */
    @Test
    @Order(100)
    void testLoginWithWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginType.USERNAME_PASSWORD);
        request.setUsername("admin");
        request.setPassword("wrongpassword");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userAppService.login(request);
        });

        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getCode());
        System.out.println("[SUCCESS] 错误密码登录被拒绝: " + exception.getMessage());
    }

    /**
     * 测试不存在的用户登录失败
     */
    @Test
    @Order(101)
    void testLoginWithNonExistentUser() {
        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginType.USERNAME_PASSWORD);
        request.setUsername("nonexistent");
        request.setPassword(DEFAULT_PASSWORD);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userAppService.login(request);
        });

        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getCode());
        System.out.println("[SUCCESS] 不存在的用户登录被拒绝: " + exception.getMessage());
    }

    /**
     * 测试空用户名登录失败
     */
    @Test
    @Order(102)
    void testLoginWithBlankUsername() {
        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginType.USERNAME_PASSWORD);
        request.setUsername(null);
        request.setPassword(DEFAULT_PASSWORD);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userAppService.login(request);
        });

        assertEquals(ErrorCode.INVALID_PARAMS, exception.getCode());
        System.out.println("[SUCCESS] 空用户名登录被拒绝: " + exception.getMessage());
    }

    /**
     * 测试空密码登录失败
     */
    @Test
    @Order(103)
    void testLoginWithBlankPassword() {
        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginType.USERNAME_PASSWORD);
        request.setUsername("admin");
        request.setPassword(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userAppService.login(request);
        });

        assertEquals(ErrorCode.INVALID_PARAMS, exception.getCode());
        System.out.println("[SUCCESS] 空密码登录被拒绝: " + exception.getMessage());
    }

    // ==================== 辅助方法 ====================

    /**
     * 执行用户名密码登录
     */
    private UserResponse performLogin(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginType.USERNAME_PASSWORD);
        request.setUsername(username);
        request.setPassword(password);

        return userAppService.login(request);
    }
}