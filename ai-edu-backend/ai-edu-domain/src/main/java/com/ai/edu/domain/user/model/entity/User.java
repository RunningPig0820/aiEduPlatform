package com.ai.edu.domain.user.model.entity;

import com.ai.edu.domain.user.service.PasswordVerifier;
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
     * 验证密码
     */
    public boolean verifyPassword(String rawPassword, PasswordVerifier verifier) {
        return verifier.matches(rawPassword, this.password);
    }

    /**
     * 设置新密码（用于密码重置等已验证身份的场景）
     */
    public void setNewPassword(String encodedNewPassword) {
        this.password = encodedNewPassword;
    }

    /**
     * 修改密码（校验旧密码、新旧不能相同）
     *
     * @return 修改结果
     */
    public PasswordChangeResult changePassword(String oldRawPassword, String newRawPassword, PasswordVerifier verifier) {
        if (!verifyPassword(oldRawPassword, verifier)) {
            return PasswordChangeResult.OLD_PASSWORD_WRONG;
        }
        if (verifier.matches(newRawPassword, this.password)) {
            return PasswordChangeResult.SAME_AS_OLD;
        }
        this.password = newRawPassword;
        return PasswordChangeResult.SUCCESS;
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

    /**
     * 密码修改结果
     */
    public enum PasswordChangeResult {
        SUCCESS,
        OLD_PASSWORD_WRONG,
        SAME_AS_OLD
    }
}