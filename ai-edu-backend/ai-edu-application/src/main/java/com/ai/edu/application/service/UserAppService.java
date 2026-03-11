package com.ai.edu.application.service;

import com.ai.edu.application.assembler.UserAssembler;
import com.ai.edu.application.dto.LoginRequest;
import com.ai.edu.application.dto.RegisterRequest;
import com.ai.edu.application.dto.UserResponse;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.common.util.PasswordUtil;
import com.ai.edu.domain.user.model.entity.User;
import com.ai.edu.domain.user.repository.UserRepository;
import com.ai.edu.domain.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户应用服务
 */
@Service
@RequiredArgsConstructor
public class UserAppService {

    private final UserRepository userRepository;
    private final UserDomainService userDomainService;

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

    public UserResponse login(LoginRequest request) {
        // 查找用户
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "用户名或密码错误"));

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

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        return UserAssembler.toResponse(user);
    }
}