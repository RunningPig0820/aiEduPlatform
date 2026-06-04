package com.ai.edu.infrastructure.integration.user;

import com.ai.edu.domain.organization.service.UserQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户域查询服务真实实现（基于 H2 数据库测试）
 * 直接查询 t_user 表，不依赖外部用户域服务
 */
@Slf4j
@Service
public class H2UserQueryService implements UserQueryService {

    private final JdbcTemplate jdbcTemplate;

    public H2UserQueryService(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        log.info("H2UserQueryService initialized for testing");
    }

    @Override
    public Optional<UserInfo> findByPhone(String phone) {
        log.info("根据手机号查询用户（H2测试）: phone={}", phone);

        String sql = "SELECT id, real_name, phone FROM t_user WHERE phone = ? AND enabled = true";

        List<UserInfo> users = jdbcTemplate.query(
                sql,
                new Object[]{phone},
                (rs, rowNum) -> new UserInfo(
                        rs.getLong("id"),
                        rs.getString("real_name"),
                        rs.getString("phone")
                )
        );

        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    @Override
    public Long createUser(String name, String phone) {
        log.info("创建用户（H2测试）: name={}, phone={}", name, phone);

        // 检查手机号是否已存在
        if (findByPhone(phone).isPresent()) {
            throw new IllegalArgumentException("手机号已存在: " + phone);
        }

        // 生成用户名（基于手机号）
        String username = "user_" + phone.substring(phone.length() - 4);

        String sql = "INSERT INTO t_user (username, password, real_name, phone, role, enabled) VALUES (?, ?, ?, ?, 'TEACHER', true)";

        jdbcTemplate.update(sql, username, "password123", name, phone);

        // 获取生成的ID
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_user WHERE phone = ?",
                new Object[]{phone},
                Long.class
        );

        log.info("用户创建成功（H2测试）: userId={}, name={}, phone={}", userId, name, phone);
        return userId;
    }

    @Override
    public List<UserInfo> findByIds(List<Long> userIds) {
        log.info("批量查询用户（H2测试）: userIds={}", userIds);

        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        String ids = userIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        String sql = "SELECT id, real_name, phone FROM t_user WHERE id IN (" + ids + ") AND enabled = true";

        List<UserInfo> users = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new UserInfo(
                        rs.getLong("id"),
                        rs.getString("real_name"),
                        rs.getString("phone")
                )
        );

        log.info("批量查询完成（H2测试）: 查询{}个，返回{}个", userIds.size(), users.size());
        return users;
    }
}