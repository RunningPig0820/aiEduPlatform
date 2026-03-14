package com.ai.edu.common.constant;

/**
 * 登录类型常量
 */
public final class LoginType {

    private LoginType() {}

    /**
     * 用户名+密码登录
     */
    public static final String USERNAME_PASSWORD = "USERNAME_PASSWORD";

    /**
     * 手机号+密码登录
     */
    public static final String PHONE_PASSWORD = "PHONE_PASSWORD";

    /**
     * 手机号+验证码登录
     */
    public static final String PHONE_CODE = "PHONE_CODE";
}