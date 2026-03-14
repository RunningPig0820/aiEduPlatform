package com.ai.edu.domain.user.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 用户实体
 */
@Entity
@Table(name = "t_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "real_name", nullable = false, length = 50)
    private String realName;

    @Column(length = 20, unique = true)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false)
    private Boolean enabled = true;

    public static User create(String username, String password, String realName, String role) {
        User user = new User();
        user.username = username;
        user.password = password;
        user.realName = realName;
        user.role = role;
        return user;
    }

    public static User create(String username, String password, String realName, String phone, String role) {
        User user = new User();
        user.username = username;
        user.password = password;
        user.realName = realName;
        user.phone = phone;
        user.role = role;
        return user;
    }

    /**
     * 修改密码
     */
    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    public void updateProfile(String realName, String phone, String email) {
        this.realName = realName;
        this.phone = phone;
        this.email = email;
    }

    public void disable() {
        this.enabled = false;
    }

    public void enable() {
        this.enabled = true;
    }
}