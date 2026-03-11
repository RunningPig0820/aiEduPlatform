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

    boolean existsByUsername(String username);

    void deleteById(Long id);
}