package com.ai.edu.domain.user.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 用户实体
 */
@TableName("t_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @com.baomidou.mybatisplus.annotation.TableId(type = IdType.AUTO)
    private Long id;

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    @TableField("real_name")
    private String realName;

    @TableField("phone")
    private String phone;

    @TableField("email")
    private String email;

    @TableField("role")
    private String role;

    @TableField("enabled")
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