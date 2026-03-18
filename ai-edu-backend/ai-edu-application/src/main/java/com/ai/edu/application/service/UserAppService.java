package com.ai.edu.application.service;

import com.ai.edu.application.assembler.UserAssembler;
import com.ai.edu.application.dto.*;
import com.ai.edu.common.constant.CodeScene;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.constant.LoginType;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.common.util.PasswordUtil;
import com.ai.edu.domain.user.model.entity.User;
import com.ai.edu.domain.user.model.valueobject.VerificationCode;
import com.ai.edu.domain.user.repository.UserRepository;
import com.ai.edu.domain.user.service.UserDomainService;
import com.ai.edu.domain.user.service.VerificationCodeService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户应用服务
 * 职责：编排领域服务，处理用例流转，不包含业务逻辑
 *
 * @author AI Edu Platform
 */
@Slf4j
@Service
public class UserAppService {

    @Resource
    private UserRepository userRepository;
    @Resource
    private UserDomainService userDomainService;
    @Resource
    private VerificationCodeService verificationCodeService;

    /**
     * 演示账号配置
     */
    private static final Map<String, DemoAccount> DEMO_ACCOUNTS = new HashMap<>();

    static {
        DEMO_ACCOUNTS.put("STUDENT", new DemoAccount(1L, "student", "演示学生", "STUDENT"));
        DEMO_ACCOUNTS.put("TEACHER", new DemoAccount(2L, "teacher", "演示老师", "TEACHER"));
        DEMO_ACCOUNTS.put("PARENT", new DemoAccount(3L, "parent", "演示家长", "PARENT"));
    }

    // ==================== 用户注册 ====================

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 用户响应
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        // 1. 验证验证码（调用领域服务）
        if (!verificationCodeService.verify(request.getPhone(), CodeScene.REGISTER, request.getCode())) {
            throw new BusinessException(ErrorCode.CODE_INVALID, "验证码错误或已过期");
        }

        // 2. 检查用户名是否可用（调用领域服务）
        if (!userDomainService.isUsernameAvailable(request.getUsername())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "用户名已存在");
        }

        // 3. 检查手机号是否可用（调用领域服务）
        if (!userDomainService.isPhoneAvailable(request.getPhone())) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED, "手机号已被注册");
        }

        // 4. 创建用户（调用领域服务）
        String encodedPassword = PasswordUtil.encode(request.getPassword());
        User user = userDomainService.createUser(
                request.getUsername(),
                encodedPassword,
                request.getRealName(),
                request.getPhone(),
                request.getRole()
        );

        // 5. 保存用户
        User savedUser = userRepository.save(user);

        return UserAssembler.toResponse(savedUser);
    }

    // ==================== 用户登录 ====================

    /**
     * 用户登录（支持三种方式）
     *
     * @param request 登录请求
     * @return 用户响应
     */
    public UserResponse login(LoginRequest request) {
        String loginType = request.getLoginType();
        User user;

        switch (loginType) {
            case LoginType.USERNAME_PASSWORD:
                user = loginByUsernameAndPassword(request);
                break;
            case LoginType.PHONE_PASSWORD:
                user = loginByPhoneAndPassword(request);
                break;
            case LoginType.PHONE_CODE:
                user = loginByPhoneAndCode(request);
                break;
            default:
                throw new BusinessException(ErrorCode.INVALID_PARAMS, "无效的登录类型");
        }

        // 检查用户状态
        if (!user.getEnabled()) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "账号已被禁用");
        }

        return UserAssembler.toResponse(user);
    }

    private User loginByUsernameAndPassword(LoginRequest request) {
        if (!StringUtils.hasText(request.getUsername())) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "用户名不能为空");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "密码不能为空");
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "用户名或密码错误"));

        if (!PasswordUtil.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "用户名或密码错误");
        }

        return user;
    }

    private User loginByPhoneAndPassword(LoginRequest request) {
        if (!StringUtils.hasText(request.getPhone())) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "手机号不能为空");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "密码不能为空");
        }

        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "手机号或密码错误"));

        if (!PasswordUtil.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "手机号或密码错误");
        }

        return user;
    }

    private User loginByPhoneAndCode(LoginRequest request) {
        if (!StringUtils.hasText(request.getPhone())) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "手机号不能为空");
        }
        if (!StringUtils.hasText(request.getCode())) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "验证码不能为空");
        }

        // 验证验证码（调用领域服务）
        if (!verificationCodeService.verify(request.getPhone(), CodeScene.LOGIN, request.getCode())) {
            throw new BusinessException(ErrorCode.CODE_INVALID, "验证码错误或已过期");
        }

        return userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_NOT_REGISTERED, "手机号未注册"));
    }

    // ==================== 演示登录 ====================

    /**
     * 演示账号快捷登录
     *
     * @param request 演示登录请求
     * @param session HTTP Session
     * @return 用户响应
     */
    public UserResponse demoLogin(DemoLoginRequest request, HttpSession session) {
        String role = request.getRole().toUpperCase();
        DemoAccount demoAccount = DEMO_ACCOUNTS.get(role);

        if (demoAccount == null) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "无效的角色类型");
        }

        // 查找或创建演示账号
        User user = userRepository.findByUsername(demoAccount.username)
                .orElseGet(() -> createDemoUser(demoAccount));

        // 设置 Session
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", user.getRole());

        log.info("演示登录成功: role={}, username={}", role, user.getUsername());

        return UserAssembler.toResponse(user);
    }

    // ==================== 验证码 ====================

    /**
     * 发送验证码
     *
     * @param phone 手机号
     * @param scene 场景
     * @return 验证码存储 Key（用于测试）
     */
    public String sendCode(String phone, String scene) {
        // 验证手机号格式
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "手机号格式不正确");
        }

        // 根据场景校验手机号状态
        validatePhoneForScene(phone, scene);

        // 生成并存储验证码（调用领域服务）
        VerificationCode verificationCode = verificationCodeService.generateAndSave(phone, scene);

        log.info("验证码已发送: phone={}, scene={}", phone, scene);
        return verificationCode.getCode();
    }

    /**
     * 根据场景校验手机号状态
     */
    private void validatePhoneForScene(String phone, String scene) {
        switch (scene) {
            case CodeScene.REGISTER:
                // 注册场景：手机号不能已注册
                if (userRepository.existsByPhone(phone)) {
                    throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED, "手机号已被注册");
                }
                break;
            case CodeScene.LOGIN:
            case CodeScene.RESET_PASSWORD:
                // 登录/重置密码场景：手机号必须已注册
                if (!userRepository.existsByPhone(phone)) {
                    throw new BusinessException(ErrorCode.PHONE_NOT_REGISTERED, "手机号未注册");
                }
                break;
            default:
                throw new BusinessException(ErrorCode.INVALID_PARAMS, "无效的验证码场景");
        }
    }

    /**
     * 获取验证码（用于测试）
     *
     * @param phone 手机号
     * @param scene 场景
     * @return 验证码
     */
    public String getCode(String phone, String scene) {
        VerificationCode verificationCode = verificationCodeService.get(phone, scene);
        return verificationCode != null ? verificationCode.getCode() : "";
    }

    // ==================== 密码管理 ====================

    /**
     * 重置密码
     *
     * @param request 重置密码请求
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // 验证验证码（调用领域服务）
        if (!verificationCodeService.verify(request.getPhone(), CodeScene.RESET_PASSWORD, request.getCode())) {
            throw new BusinessException(ErrorCode.CODE_INVALID, "验证码错误或已过期");
        }

        // 查找用户
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_NOT_REGISTERED, "手机号未注册"));

        // 修改密码
        String encodedPassword = PasswordUtil.encode(request.getNewPassword());
        user.changePassword(encodedPassword);

        userRepository.save(user);

        log.info("密码重置成功: phone={}", request.getPhone());
    }

    /**
     * 修改密码
     *
     * @param request 修改密码请求
     * @param session  HTTP Session
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

        // 验证原密码
        if (!PasswordUtil.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_WRONG, "原密码错误");
        }

        // 检查新密码是否与原密码相同
        if (PasswordUtil.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_SAME_AS_OLD, "新密码不能与原密码相同");
        }

        // 修改密码
        String encodedPassword = PasswordUtil.encode(request.getNewPassword());
        user.changePassword(encodedPassword);

        userRepository.save(user);

        log.info("密码修改成功: userId={}", userId);
    }

    // ==================== 用户查询 ====================

    /**
     * 获取当前登录用户
     *
     * @param session HTTP Session
     * @return 用户响应
     */
    public UserResponse getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

        return UserAssembler.toResponse(user);
    }

    /**
     * 登出
     *
     * @param session HTTP Session
     */
    public void logout(HttpSession session) {
        session.invalidate();
        log.info("用户已登出");
    }

    /**
     * 根据ID获取用户
     *
     * @param id 用户ID
     * @return 用户响应
     */
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        return UserAssembler.toResponse(user);
    }

    // ==================== 私有方法 ====================

    @Transactional
    protected User createDemoUser(DemoAccount demoAccount) {
        String encodedPassword = PasswordUtil.encode("123456");
        User user = userDomainService.createUser(
                demoAccount.username,
                encodedPassword,
                demoAccount.realName,
                demoAccount.role
        );
        return userRepository.save(user);
    }

    private record DemoAccount(Long id, String username, String realName, String role) {}
}