package com.ai.edu.domain.user.service;

import com.ai.edu.domain.user.model.valueobject.VerificationCode;
import com.ai.edu.domain.user.repository.VerificationCodeRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 验证码领域服务
 *
 * @author AI Edu Platform
 */
@Slf4j
@Service
public class VerificationCodeService {

    /**
     * 默认过期时间（分钟）
     */
    private static final int DEFAULT_EXPIRE_MINUTES = 5;

    @Resource
    private VerificationCodeRepository verificationCodeRepository;

    /**
     * 生成并存储验证码
     *
     * @param phone 手机号
     * @param scene 场景
     * @return 验证码值对象
     */
    public VerificationCode generateAndSave(String phone, String scene) {
        return generateAndSave(phone, scene, DEFAULT_EXPIRE_MINUTES);
    }

    /**
     * 生成并存储验证码（指定过期时间）
     *
     * @param phone         手机号
     * @param scene         场景
     * @param expireMinutes 过期时间（分钟）
     * @return 验证码值对象
     */
    public VerificationCode generateAndSave(String phone, String scene, int expireMinutes) {
        VerificationCode verificationCode = VerificationCode.generate(phone, scene, expireMinutes);
        verificationCodeRepository.save(verificationCode);
        log.info("验证码已生成: phone={}, scene={}, expireMinutes={}", phone, scene, expireMinutes);
        return verificationCode;
    }

    /**
     * 获取验证码
     *
     * @param phone 手机号
     * @param scene 场景
     * @return 验证码值对象，不存在返回 null
     */
    public VerificationCode get(String phone, String scene) {
        return verificationCodeRepository.findByPhoneAndScene(phone, scene);
    }

    /**
     * 验证验证码
     * 验证成功后会自动删除验证码
     *
     * @param phone 手机号
     * @param scene 场景
     * @param code  待验证的验证码
     * @return 是否验证成功
     */
    public boolean verify(String phone, String scene, String code) {
        boolean result = verificationCodeRepository.verifyAndDelete(phone, scene, code);
        if (result) {
            log.info("验证码验证成功: phone={}, scene={}", phone, scene);
        } else {
            log.warn("验证码验证失败: phone={}, scene={}", phone, scene);
        }
        return result;
    }

    /**
     * 删除验证码
     *
     * @param phone 手机号
     * @param scene 场景
     */
    public void delete(String phone, String scene) {
        verificationCodeRepository.delete(phone, scene);
        log.info("验证码已删除: phone={}, scene={}", phone, scene);
    }
}