package com.ai.edu.interface_.api;

import com.ai.edu.application.dto.*;
import com.ai.edu.application.service.UserAppService;
import com.ai.edu.common.constant.CodeScene;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.constant.LoginType;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.common.util.PasswordUtil;
import com.ai.edu.domain.user.model.entity.User;
import com.ai.edu.domain.user.repository.UserRepository;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuthApiController 集成测试
 * 使用 @Transactional 实现数据库回滚
 * 直接注入 Controller 调用方法验证
 *
 * 业务场景覆盖：
 * 1. 用户注册（注册成功、验证码错误、用户名已存在、手机号已注册、参数校验失败）
 * 2. 用户登录 - 用户名+密码（成功、用户不存在、密码错误、账号禁用）
 * 3. 用户登录 - 手机号+密码（成功、手机号未注册、密码错误）
 * 4. 用户登录 - 手机号+验证码（成功、验证码错误、手机号未注册）
 * 5. 演示登录（STUDENT/TEACHER/PARENT角色、无效角色）
 * 6. 发送验证码（各场景成功、手机号格式错误、业务校验失败）
 * 7. 重置密码（成功、验证码错误、手机号未注册）
 * 8. 修改密码（成功、未登录、原密码错误、新密码与原密码相同）
 * 9. 登出（成功）
 * 10. 获取当前用户（成功、未登录）
 * 11. 获取用户信息（成功、用户不存在）
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthApiControllerTest {

    @Resource
    private AuthApiController authApiController;
    @Resource
    private UserAppService userAppService;
    @Resource
    private UserRepository userRepository;

    // 测试数据常量
    private static final String TEST_PHONE = "13800138001";
    private static final String TEST_PHONE_2 = "13800138002";
    private static final String TEST_USERNAME = "testuser001";
    private static final String TEST_USERNAME_2 = "testuser002";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_REAL_NAME = "测试用户";
    private static final String WRONG_CODE = "654321";
    private static final String WRONG_PASSWORD = "wrongpassword";
    private static final String NEW_PASSWORD = "newpassword123";

    // ==================== 用户注册测试 ====================

    /**
     * 场景1.1: 注册成功 - 正常注册流程
     */
    @Test
    @Order(100)
    @Transactional
    void register_Success() {
        // 1. 先发送验证码
        SendCodeRequest sendCodeRequest = new SendCodeRequest();
        sendCodeRequest.setPhone(TEST_PHONE);
        sendCodeRequest.setScene(CodeScene.REGISTER);

        ApiResponse<Void> sendResult = authApiController.sendCode(sendCodeRequest);
        assertEquals(ErrorCode.SUCCESS, sendResult.getCode());

        String code = userAppService.getCode(sendCodeRequest.getPhone(), sendCodeRequest.getScene());

        // 2. 注册
        RegisterRequest request = new RegisterRequest();
        request.setUsername(TEST_USERNAME);
        request.setPassword(TEST_PASSWORD);
        request.setRealName(TEST_REAL_NAME);
        request.setPhone(TEST_PHONE);
        request.setCode(code);
        request.setRole("STUDENT");

        ApiResponse<UserResponse> response = authApiController.register(request);

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        assertNotNull(response.getData());
        assertEquals(TEST_USERNAME, response.getData().getUsername());
        assertEquals(TEST_REAL_NAME, response.getData().getRealName());
        assertEquals(TEST_PHONE, response.getData().getPhone());
        assertEquals("STUDENT", response.getData().getRole());
    }

    /**
     * 场景1.2: 注册失败 - 验证码错误
     */
    @Test
    @Order(101)
    @Transactional
    void register_Fail_WrongCode() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(TEST_USERNAME);
        request.setPassword(TEST_PASSWORD);
        request.setRealName(TEST_REAL_NAME);
        request.setPhone(TEST_PHONE);
        request.setCode(WRONG_CODE);
        request.setRole("STUDENT");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.register(request);
        });
        assertEquals(ErrorCode.CODE_INVALID, exception.getCode());
    }

    /**
     * 场景1.3: 注册失败 - 用户名已存在
     */
    @Test
    @Order(102)
    @Transactional
    void register_Fail_UsernameExists() {
        // 1. 创建一个用户
        createTestUser(TEST_USERNAME, TEST_PHONE);

        // 2. 发送验证码
        SendCodeRequest sendCodeRequest = new SendCodeRequest();
        sendCodeRequest.setPhone(TEST_PHONE_2);
        sendCodeRequest.setScene(CodeScene.REGISTER);
        authApiController.sendCode(sendCodeRequest);


        String code = userAppService.getCode(sendCodeRequest.getPhone(), sendCodeRequest.getScene());


        // 3. 尝试用相同用户名注册
        RegisterRequest request = new RegisterRequest();
        request.setUsername(TEST_USERNAME);
        request.setPassword(TEST_PASSWORD);
        request.setRealName(TEST_REAL_NAME);
        request.setPhone(TEST_PHONE_2);
        request.setCode(code);
        request.setRole("STUDENT");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.register(request);
        });
        assertEquals(ErrorCode.USER_ALREADY_EXISTS, exception.getCode());
    }

    /**
     * 场景1.4: 注册失败 - 手机号已注册（发送验证码时校验）
     */
    @Test
    @Order(103)
    @Transactional
    void register_Fail_PhoneRegistered() {
        // 1. 创建一个用户
        createTestUser(TEST_USERNAME, TEST_PHONE);

        // 2. 尝试用相同手机号发送验证码
        SendCodeRequest sendCodeRequest = new SendCodeRequest();
        sendCodeRequest.setPhone(TEST_PHONE);
        sendCodeRequest.setScene(CodeScene.REGISTER);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.sendCode(sendCodeRequest);
        });
        assertEquals(ErrorCode.PHONE_ALREADY_REGISTERED, exception.getCode());
    }

    // ==================== 用户名+密码登录测试 ====================

    /**
     * 场景2.1: 用户名+密码登录成功
     */
    @Test
    @Order(200)
    @Transactional
    void login_UsernamePassword_Success() {
        // 1. 创建测试用户
        createTestUser(TEST_USERNAME, TEST_PHONE);

        // 2. 登录
        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginType.USERNAME_PASSWORD);
        request.setUsername(TEST_USERNAME);
        request.setPassword(TEST_PASSWORD);

        HttpSession session = new MockHttpSession();
        ApiResponse<UserResponse> response = authApiController.login(request, session);

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        assertNotNull(response.getData());
        assertEquals(TEST_USERNAME, response.getData().getUsername());

        // 验证 Session
        assertNotNull(session.getAttribute("userId"));
        assertEquals(TEST_USERNAME, session.getAttribute("username"));
    }

    /**
     * 场景2.2: 用户名+密码登录失败 - 用户名不存在
     */
    @Test
    @Order(201)
    @Transactional
    void login_UsernamePassword_Fail_UserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginType.USERNAME_PASSWORD);
        request.setUsername("nonexistent");
        request.setPassword(TEST_PASSWORD);

        HttpSession session = new MockHttpSession();
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.login(request, session);
        });
        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getCode());
    }

    /**
     * 场景2.3: 用户名+密码登录失败 - 密码错误
     */
    @Test
    @Order(202)
    @Transactional
    void login_UsernamePassword_Fail_WrongPassword() {
        createTestUser(TEST_USERNAME, TEST_PHONE);

        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginType.USERNAME_PASSWORD);
        request.setUsername(TEST_USERNAME);
        request.setPassword(WRONG_PASSWORD);

        HttpSession session = new MockHttpSession();
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.login(request, session);
        });
        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getCode());
    }

    /**
     * 场景2.4: 用户名+密码登录失败 - 用户名为空
     */
    @Test
    @Order(203)
    @Transactional
    void login_UsernamePassword_Fail_UsernameBlank() {
        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginType.USERNAME_PASSWORD);
        request.setUsername(null);
        request.setPassword(TEST_PASSWORD);

        HttpSession session = new MockHttpSession();
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.login(request, session);
        });
        assertEquals(ErrorCode.INVALID_PARAMS, exception.getCode());
    }

    /**
     * 场景2.5: 用户名+密码登录失败 - 密码为空
     */
    @Test
    @Order(204)
    @Transactional
    void login_UsernamePassword_Fail_PasswordBlank() {
        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginType.USERNAME_PASSWORD);
        request.setUsername(TEST_USERNAME);
        request.setPassword(null);

        HttpSession session = new MockHttpSession();
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.login(request, session);
        });
        assertEquals(ErrorCode.INVALID_PARAMS, exception.getCode());
    }

    /**
     * 场景2.6: 用户名+密码登录失败 - 账号被禁用
     */
    @Test
    @Order(205)
    @Transactional
    void login_UsernamePassword_Fail_AccountDisabled() {
        // 创建并禁用用户
        User user = createTestUser(TEST_USERNAME, TEST_PHONE);
        user.disable();
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginType.USERNAME_PASSWORD);
        request.setUsername(TEST_USERNAME);
        request.setPassword(TEST_PASSWORD);

        HttpSession session = new MockHttpSession();
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.login(request, session);
        });
        assertEquals(ErrorCode.PERMISSION_DENIED, exception.getCode());
    }

    // ==================== 手机号+密码登录测试 ====================

    /**
     * 场景3.1: 手机号+密码登录成功
     */
    @Test
    @Order(300)
    @Transactional
    void login_PhonePassword_Success() {
        createTestUser(TEST_USERNAME, TEST_PHONE);

        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginType.PHONE_PASSWORD);
        request.setPhone(TEST_PHONE);
        request.setPassword(TEST_PASSWORD);

        HttpSession session = new MockHttpSession();
        ApiResponse<UserResponse> response = authApiController.login(request, session);

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        assertEquals(TEST_PHONE, response.getData().getPhone());
    }

    /**
     * 场景3.2: 手机号+密码登录失败 - 手机号未注册
     */
    @Test
    @Order(301)
    @Transactional
    void login_PhonePassword_Fail_PhoneNotRegistered() {
        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginType.PHONE_PASSWORD);
        request.setPhone(TEST_PHONE);
        request.setPassword(TEST_PASSWORD);

        HttpSession session = new MockHttpSession();
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.login(request, session);
        });
        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getCode());
    }

    /**
     * 场景3.3: 手机号+密码登录失败 - 密码错误
     */
    @Test
    @Order(302)
    @Transactional
    void login_PhonePassword_Fail_WrongPassword() {
        createTestUser(TEST_USERNAME, TEST_PHONE);

        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginType.PHONE_PASSWORD);
        request.setPhone(TEST_PHONE);
        request.setPassword(WRONG_PASSWORD);

        HttpSession session = new MockHttpSession();
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.login(request, session);
        });
        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getCode());
    }

    /**
     * 场景3.4: 手机号+密码登录失败 - 手机号为空
     */
    @Test
    @Order(303)
    @Transactional
    void login_PhonePassword_Fail_PhoneBlank() {
        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginType.PHONE_PASSWORD);
        request.setPhone(null);
        request.setPassword(TEST_PASSWORD);

        HttpSession session = new MockHttpSession();
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.login(request, session);
        });
        assertEquals(ErrorCode.INVALID_PARAMS, exception.getCode());
    }

    // ==================== 手机号+验证码登录测试 ====================

    /**
     * 场景4.1: 手机号+验证码登录成功
     */
    @Test
    @Order(400)
    @Transactional
    void login_PhoneCode_Success() {
        createTestUser(TEST_USERNAME, TEST_PHONE);

        // 发送验证码
        SendCodeRequest sendCodeRequest = new SendCodeRequest();
        sendCodeRequest.setPhone(TEST_PHONE);
        sendCodeRequest.setScene(CodeScene.LOGIN);
        authApiController.sendCode(sendCodeRequest);

        String code = userAppService.getCode(TEST_PHONE, CodeScene.LOGIN);

        // 登录
        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginType.PHONE_CODE);
        request.setPhone(TEST_PHONE);
        request.setCode(code);

        HttpSession session = new MockHttpSession();
        ApiResponse<UserResponse> response = authApiController.login(request, session);

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        assertEquals(TEST_PHONE, response.getData().getPhone());
    }

    /**
     * 场景4.2: 手机号+验证码登录失败 - 验证码错误
     */
    @Test
    @Order(401)
    @Transactional
    void login_PhoneCode_Fail_WrongCode() {
        createTestUser(TEST_USERNAME, TEST_PHONE);

        LoginRequest request = new LoginRequest();
        request.setLoginType(LoginType.PHONE_CODE);
        request.setPhone(TEST_PHONE);
        request.setCode(WRONG_CODE);

        HttpSession session = new MockHttpSession();
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.login(request, session);
        });
        assertEquals(ErrorCode.CODE_INVALID, exception.getCode());
    }

    /**
     * 场景4.3: 手机号+验证码登录失败 - 手机号未注册（发送验证码时校验）
     */
    @Test
    @Order(402)
    @Transactional
    void login_PhoneCode_Fail_PhoneNotRegistered() {
        SendCodeRequest sendCodeRequest = new SendCodeRequest();
        sendCodeRequest.setPhone(TEST_PHONE);
        sendCodeRequest.setScene(CodeScene.LOGIN);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.sendCode(sendCodeRequest);
        });
        assertEquals(ErrorCode.PHONE_NOT_REGISTERED, exception.getCode());
    }

    // ==================== 演示登录测试 ====================

    /**
     * 场景5.1: 演示登录成功 - STUDENT角色
     */
    @Test
    @Order(500)
    @Transactional
    void demoLogin_Success_Student() {
        DemoLoginRequest request = new DemoLoginRequest();
        request.setRole("STUDENT");

        HttpSession session = new MockHttpSession();
        ApiResponse<UserResponse> response = authApiController.demoLogin(request, session);

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        assertEquals("STUDENT", response.getData().getRole());
    }

    /**
     * 场景5.2: 演示登录成功 - TEACHER角色
     */
    @Test
    @Order(501)
    @Transactional
    void demoLogin_Success_Teacher() {
        DemoLoginRequest request = new DemoLoginRequest();
        request.setRole("TEACHER");

        HttpSession session = new MockHttpSession();
        ApiResponse<UserResponse> response = authApiController.demoLogin(request, session);

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        assertEquals("TEACHER", response.getData().getRole());
    }

    /**
     * 场景5.3: 演示登录成功 - PARENT角色
     */
    @Test
    @Order(502)
    @Transactional
    void demoLogin_Success_Parent() {
        DemoLoginRequest request = new DemoLoginRequest();
        request.setRole("PARENT");

        HttpSession session = new MockHttpSession();
        ApiResponse<UserResponse> response = authApiController.demoLogin(request, session);

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        assertEquals("PARENT", response.getData().getRole());
    }

    /**
     * 场景5.4: 演示登录失败 - 无效角色
     */
    @Test
    @Order(503)
    @Transactional
    void demoLogin_Fail_InvalidRole() {
        DemoLoginRequest request = new DemoLoginRequest();
        request.setRole("INVALID_ROLE");

        HttpSession session = new MockHttpSession();
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.demoLogin(request, session);
        });
        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getCode());
    }

    // ==================== 发送验证码测试 ====================

    /**
     * 场景6.1: 发送验证码成功 - 注册场景
     */
    @Test
    @Order(600)
    @Transactional
    void sendCode_Success_Register() {
        SendCodeRequest request = new SendCodeRequest();
        request.setPhone(TEST_PHONE);
        request.setScene(CodeScene.REGISTER);

        ApiResponse<Void> response = authApiController.sendCode(request);

        assertEquals(ErrorCode.SUCCESS, response.getCode());
    }

    /**
     * 场景6.2: 发送验证码成功 - 登录场景
     */
    @Test
    @Order(601)
    @Transactional
    void sendCode_Success_Login() {
        createTestUser(TEST_USERNAME, TEST_PHONE);

        SendCodeRequest request = new SendCodeRequest();
        request.setPhone(TEST_PHONE);
        request.setScene(CodeScene.LOGIN);

        ApiResponse<Void> response = authApiController.sendCode(request);

        assertEquals(ErrorCode.SUCCESS, response.getCode());
    }

    /**
     * 场景6.3: 发送验证码成功 - 重置密码场景
     */
    @Test
    @Order(602)
    @Transactional
    void sendCode_Success_ResetPassword() {
        createTestUser(TEST_USERNAME, TEST_PHONE);

        SendCodeRequest request = new SendCodeRequest();
        request.setPhone(TEST_PHONE);
        request.setScene(CodeScene.RESET_PASSWORD);

        ApiResponse<Void> response = authApiController.sendCode(request);

        assertEquals(ErrorCode.SUCCESS, response.getCode());
    }

    /**
     * 场景6.4: 发送验证码失败 - 手机号格式错误
     */
    @Test
    @Order(603)
    @Transactional
    void sendCode_Fail_InvalidPhoneFormat() {
        SendCodeRequest request = new SendCodeRequest();
        request.setPhone("12345678");
        request.setScene(CodeScene.REGISTER);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.sendCode(request);
        });
        assertEquals(ErrorCode.INVALID_PARAMS, exception.getCode());
    }

    /**
     * 场景6.5: 发送验证码失败 - 注册场景手机号已存在
     */
    @Test
    @Order(604)
    @Transactional
    void sendCode_Fail_RegisterPhoneExists() {
        createTestUser(TEST_USERNAME, TEST_PHONE);

        SendCodeRequest request = new SendCodeRequest();
        request.setPhone(TEST_PHONE);
        request.setScene(CodeScene.REGISTER);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.sendCode(request);
        });
        assertEquals(ErrorCode.PHONE_ALREADY_REGISTERED, exception.getCode());
    }

    /**
     * 场景6.6: 发送验证码失败 - 登录场景手机号未注册
     */
    @Test
    @Order(605)
    @Transactional
    void sendCode_Fail_LoginPhoneNotRegistered() {
        SendCodeRequest request = new SendCodeRequest();
        request.setPhone(TEST_PHONE);
        request.setScene(CodeScene.LOGIN);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.sendCode(request);
        });
        assertEquals(ErrorCode.PHONE_NOT_REGISTERED, exception.getCode());
    }

    /**
     * 场景6.7: 发送验证码失败 - 无效场景
     */
    @Test
    @Order(606)
    @Transactional
    void sendCode_Fail_InvalidScene() {
        SendCodeRequest request = new SendCodeRequest();
        request.setPhone(TEST_PHONE);
        request.setScene("INVALID_SCENE");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.sendCode(request);
        });
        assertEquals(ErrorCode.INVALID_PARAMS, exception.getCode());
    }

    // ==================== 重置密码测试 ====================

    /**
     * 场景7.1: 重置密码成功
     */
    @Test
    @Order(700)
    @Transactional
    void resetPassword_Success() {
        createTestUser(TEST_USERNAME, TEST_PHONE);

        // 发送验证码
        SendCodeRequest sendCodeRequest = new SendCodeRequest();
        sendCodeRequest.setPhone(TEST_PHONE);
        sendCodeRequest.setScene(CodeScene.RESET_PASSWORD);
        authApiController.sendCode(sendCodeRequest);


        String code = userAppService.getCode(TEST_PHONE, CodeScene.LOGIN);

        // 重置密码
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPhone(TEST_PHONE);
        request.setCode(code);
        request.setNewPassword(NEW_PASSWORD);

        ApiResponse<Void> response = authApiController.resetPassword(request);

        assertEquals(ErrorCode.SUCCESS, response.getCode());

        // 验证可以用新密码登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginType(LoginType.PHONE_PASSWORD);
        loginRequest.setPhone(TEST_PHONE);
        loginRequest.setPassword(NEW_PASSWORD);

        HttpSession session = new MockHttpSession();
        ApiResponse<UserResponse> loginResponse = authApiController.login(loginRequest, session);
        assertEquals(ErrorCode.SUCCESS, loginResponse.getCode());
    }

    /**
     * 场景7.2: 重置密码失败 - 验证码错误
     */
    @Test
    @Order(701)
    @Transactional
    void resetPassword_Fail_WrongCode() {
        createTestUser(TEST_USERNAME, TEST_PHONE);

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPhone(TEST_PHONE);
        request.setCode(WRONG_CODE);
        request.setNewPassword(NEW_PASSWORD);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.resetPassword(request);
        });
        assertEquals(ErrorCode.CODE_INVALID, exception.getCode());
    }

    // ==================== 修改密码测试 ====================

    /**
     * 场景8.1: 修改密码成功
     */
    @Test
    @Order(800)
    @Transactional
    void changePassword_Success() {
        User user = createTestUser(TEST_USERNAME, TEST_PHONE);
        HttpSession session = createLoginSession(user);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword(TEST_PASSWORD);
        request.setNewPassword(NEW_PASSWORD);

        ApiResponse<Void> response = authApiController.changePassword(request, session);

        assertEquals(ErrorCode.SUCCESS, response.getCode());

        // 验证可以用新密码登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginType(LoginType.USERNAME_PASSWORD);
        loginRequest.setUsername(TEST_USERNAME);
        loginRequest.setPassword(NEW_PASSWORD);

        HttpSession newSession = new MockHttpSession();
        ApiResponse<UserResponse> loginResponse = authApiController.login(loginRequest, newSession);
        assertEquals(ErrorCode.SUCCESS, loginResponse.getCode());
    }

    /**
     * 场景8.2: 修改密码失败 - 未登录
     */
    @Test
    @Order(801)
    @Transactional
    void changePassword_Fail_NotLoggedIn() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword(TEST_PASSWORD);
        request.setNewPassword(NEW_PASSWORD);

        HttpSession session = new MockHttpSession();

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.changePassword(request, session);
        });
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getCode());
    }

    /**
     * 场景8.3: 修改密码失败 - 原密码错误
     */
    @Test
    @Order(802)
    @Transactional
    void changePassword_Fail_WrongOldPassword() {
        User user = createTestUser(TEST_USERNAME, TEST_PHONE);
        HttpSession session = createLoginSession(user);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword(WRONG_PASSWORD);
        request.setNewPassword(NEW_PASSWORD);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.changePassword(request, session);
        });
        assertEquals(ErrorCode.OLD_PASSWORD_WRONG, exception.getCode());
    }

    /**
     * 场景8.4: 修改密码失败 - 新密码与原密码相同
     */
    @Test
    @Order(803)
    @Transactional
    void changePassword_Fail_SameAsOld() {
        User user = createTestUser(TEST_USERNAME, TEST_PHONE);
        HttpSession session = createLoginSession(user);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword(TEST_PASSWORD);
        request.setNewPassword(TEST_PASSWORD);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.changePassword(request, session);
        });
        assertEquals(ErrorCode.PASSWORD_SAME_AS_OLD, exception.getCode());
    }

    // ==================== 登出测试 ====================

    /**
     * 场景9.1: 登出成功
     */
    @Test
    @Order(900)
    @Transactional
    void logout_Success() {
        User user = createTestUser(TEST_USERNAME, TEST_PHONE);
        HttpSession session = createLoginSession(user);

        ApiResponse<Void> response = authApiController.logout(session);

        assertEquals(ErrorCode.SUCCESS, response.getCode());
    }

    // ==================== 获取当前用户测试 ====================

    /**
     * 场景10.1: 获取当前用户成功
     */
    @Test
    @Order(1000)
    @Transactional
    void getCurrentUser_Success() {
        User user = createTestUser(TEST_USERNAME, TEST_PHONE);
        HttpSession session = createLoginSession(user);

        ApiResponse<UserResponse> response = authApiController.getCurrentUser(session);

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        assertEquals(TEST_USERNAME, response.getData().getUsername());
    }

    /**
     * 场景10.2: 获取当前用户失败 - 未登录
     */
    @Test
    @Order(1001)
    @Transactional
    void getCurrentUser_Fail_NotLoggedIn() {
        HttpSession session = new MockHttpSession();

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.getCurrentUser(session);
        });
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getCode());
    }

    // ==================== 获取用户信息测试 ====================

    /**
     * 场景11.1: 获取用户信息成功
     */
    @Test
    @Order(1100)
    @Transactional
    void getUser_Success() {
        User user = createTestUser(TEST_USERNAME, TEST_PHONE);

        ApiResponse<UserResponse> response = authApiController.getUser(user.getId());

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        assertEquals(TEST_USERNAME, response.getData().getUsername());
    }

    /**
     * 场景11.2: 获取用户信息失败 - 用户不存在
     */
    @Test
    @Order(1101)
    @Transactional
    void getUser_Fail_UserNotFound() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.getUser(999999L);
        });
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getCode());
    }

    // ==================== 无效登录类型测试 ====================

    /**
     * 场景12.1: 登录失败 - 无效的登录类型
     */
    @Test
    @Order(1200)
    @Transactional
    void login_Fail_InvalidLoginType() {
        LoginRequest request = new LoginRequest();
        request.setLoginType("INVALID_TYPE");
        request.setUsername(TEST_USERNAME);
        request.setPassword(TEST_PASSWORD);

        HttpSession session = new MockHttpSession();
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authApiController.login(request, session);
        });
        assertEquals(ErrorCode.INVALID_PARAMS, exception.getCode());
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试用户
     */
    private User createTestUser(String username, String phone) {
        User user = User.create(
                username,
                PasswordUtil.encode(TEST_PASSWORD),
                TEST_REAL_NAME,
                phone,
                "STUDENT"
        );
        return userRepository.save(user);
    }

    /**
     * 创建登录会话
     */
    private HttpSession createLoginSession(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", user.getRole());
        return session;
    }
}