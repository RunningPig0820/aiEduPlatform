package com.ai.edu.domain.user.repository;

import com.ai.edu.domain.user.model.entity.User;
import java.util.Optional;

/**
 * 用户仓储接口
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByPhone(String phone);

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);

    void deleteById(Long id);
}