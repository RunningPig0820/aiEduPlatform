package com.ai.edu.domain.user.repository;

import com.ai.edu.domain.user.model.valueobject.VerificationCode;

/**
 * 验证码仓储接口
 *
 * @author AI Edu Platform
 */
public interface VerificationCodeRepository {

    /**
     * 存储验证码
     *
     * @param verificationCode 验证码值对象
     */
    void save(VerificationCode verificationCode);

    /**
     * 根据手机号和场景获取验证码
     *
     * @param phone 手机号
     * @param scene 场景
     * @return 验证码值对象，不存在返回 null
     */
    VerificationCode findByPhoneAndScene(String phone, String scene);

    /**
     * 验证并删除验证码
     * 如果验证成功，会自动删除验证码
     *
     * @param phone    手机号
     * @param scene    场景
     * @param code     待验证的验证码
     * @return 是否验证成功
     */
    boolean verifyAndDelete(String phone, String scene, String code);

    /**
     * 删除验证码
     *
     * @param phone 手机号
     * @param scene 场景
     */
    void delete(String phone, String scene);
}