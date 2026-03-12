package com.ai.edu.application.service;

import com.ai.edu.application.assembler.UserAssembler;
import com.ai.edu.application.dto.DemoLoginRequest;
import com.ai.edu.application.dto.LoginRequest;
import com.ai.edu.application.dto.RegisterRequest;
import com.ai.edu.application.dto.UserResponse;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.common.util.PasswordUtil;
import com.ai.edu.domain.user.model.entity.User;
import com.ai.edu.domain.user.repository.UserRepository;
import com.ai.edu.domain.user.service.UserDomainService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAppService {

    private final UserRepository userRepository;
    private final UserDomainService userDomainService;

    /**
     * todo
     * 模拟验证码存储（生产环境应使用 Redis）
     */
    private static final Map<String, String> CODE_STORE = new ConcurrentHashMap<>();

    /**
     * 演示账号配置
     */
    private static final Map<String, DemoAccount> DEMO_ACCOUNTS = new HashMap<>();

    static {
        DEMO_ACCOUNTS.put("STUDENT", new DemoAccount(1L, "student", "演示学生", "STUDENT"));
        DEMO_ACCOUNTS.put("TEACHER", new DemoAccount(2L, "teacher", "演示老师", "TEACHER"));
        DEMO_ACCOUNTS.put("PARENT", new DemoAccount(3L, "parent", "演示家长", "PARENT"));
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        // 检查用户名是否可用
        if (!userDomainService.isUsernameAvailable(request.getUsername())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "用户名已存在");
        }

        // 创建用户
        String encodedPassword = PasswordUtil.encode(request.getPassword());
        User user = userDomainService.createUser(
                request.getUsername(),
                encodedPassword,
                request.getRealName(),
                request.getRole()
        );

        // 保存用户
        User savedUser = userRepository.save(user);

        return UserAssembler.toResponse(savedUser);
    }

    /**
     * 用户登录（支持用户名或手机号）
     */
    public UserResponse login(LoginRequest request) {
        // 确定登录标识（用户名或手机号）
        String identifier = StringUtils.hasText(request.getUsername())
                ? request.getUsername()
                : request.getPhone();

        if (!StringUtils.hasText(identifier)) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "请输入用户名或手机号");
        }

        // 查找用户
        User user = findUserByIdentifier(request.getUsername(), request.getPhone());

        // 验证密码
        if (!PasswordUtil.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "用户名或密码错误");
        }

        // 检查用户状态
        if (!user.getEnabled()) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "账号已被禁用");
        }

        return UserAssembler.toResponse(user);
    }

    /**
     * 演示账号快捷登录
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

    /**
     * 发送验证码（模拟）
     */
    public void sendCode(String phone) {
        // 验证手机号格式
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "手机号格式不正确");
        }

        // 生成6位验证码
        String code = String.format("%06d", new Random().nextInt(1000000));

        // 存储验证码（生产环境应使用 Redis，设置过期时间）
        CODE_STORE.put(phone, code);

        // 模拟发送（生产环境调用短信服务）
        log.info("验证码已发送: phone={}, code={}", phone, code);
    }

    /**
     * 验证验证码
     */
    public boolean verifyCode(String phone, String code) {
        String storedCode = CODE_STORE.get(phone);
        if (storedCode != null && storedCode.equals(code)) {
            CODE_STORE.remove(phone);
            return true;
        }
        return false;
    }

    /**
     * 获取当前登录用户
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
     */
    public void logout(HttpSession session) {
        session.invalidate();
        log.info("用户已登出");
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        return UserAssembler.toResponse(user);
    }

    /**
     * 根据用户名或手机号查找用户
     */
    private User findUserByIdentifier(String username, String phone) {
        if (StringUtils.hasText(username)) {
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "用户名或密码错误"));
        } else if (StringUtils.hasText(phone)) {
            return userRepository.findByPhone(phone)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "手机号或密码错误"));
        }
        throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "请输入用户名或手机号");
    }

    /**
     * 创建演示用户
     */
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

    /**
     * 演示账号配置
     */
    private record DemoAccount(Long id, String username, String realName, String role) {}
}