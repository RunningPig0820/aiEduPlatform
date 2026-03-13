package com.ai.edu.domain.user.service;

import com.ai.edu.domain.user.model.entity.User;
import com.ai.edu.domain.user.repository.UserRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 用户领域服务
 */
@Service
public class UserDomainService {

    @Resource
    private UserRepository userRepository;

    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    public User createUser(String username, String password, String realName, String role) {
        if (!isUsernameAvailable(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        return User.create(username, password, realName, role);
    }
}