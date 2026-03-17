package com.ai.edu.domain.user.model.valueobject;

import lombok.Getter;

import java.security.SecureRandom;

/**
 * 验证码值对象
 *
 * @author AI Edu Platform
 */
@Getter
public class VerificationCode {

    /**
     * 验证码内容
     */
    private final String code;

    /**
     * 手机号
     */
    private final String phone;

    /**
     * 场景
     */
    private final String scene;

    /**
     * 过期时间（分钟）
     */
    private final int expireMinutes;

    private static final SecureRandom RANDOM = new SecureRandom();

    private VerificationCode(String phone, String scene, String code, int expireMinutes) {
        this.phone = phone;
        this.scene = scene;
        this.code = code;
        this.expireMinutes = expireMinutes;
    }

    /**
     * 生成6位随机验证码
     *
     * @param phone         手机号
     * @param scene         场景
     * @param expireMinutes 过期时间（分钟）
     * @return 验证码值对象
     */
    public static VerificationCode generate(String phone, String scene, int expireMinutes) {
        String code = String.format("%06d", RANDOM.nextInt(1000000));
        return new VerificationCode(phone, scene, code, expireMinutes);
    }

    /**
     * 生成6位随机验证码（默认5分钟过期）
     *
     * @param phone 手机号
     * @param scene 场景
     * @return 验证码值对象
     */
    public static VerificationCode generate(String phone, String scene) {
        return generate(phone, scene, 5);
    }

    /**
     * 创建用于验证的验证码实例（从存储中恢复）
     *
     * @param phone 手机号
     * @param scene 场景
     * @param code  验证码
     * @return 验证码值对象
     */
    public static VerificationCode of(String phone, String scene, String code) {
        return new VerificationCode(phone, scene, code, 0);
    }

    /**
     * 验证输入的验证码是否正确
     *
     * @param inputCode 输入的验证码
     * @return 是否正确
     */
    public boolean matches(String inputCode) {
        return this.code != null && this.code.equals(inputCode);
    }

    /**
     * 构建 Redis 存储的 Key
     *
     * @return Redis Key
     */
    public String toStorageKey() {
        return "ai-edu:code:" + phone + ":" + scene;
    }

    @Override
    public String toString() {
        return "VerificationCode{phone='" + phone + "', scene='" + scene + "', expireMinutes=" + expireMinutes + "}";
    }
}