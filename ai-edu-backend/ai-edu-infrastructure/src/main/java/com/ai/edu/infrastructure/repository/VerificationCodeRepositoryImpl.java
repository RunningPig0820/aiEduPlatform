package com.ai.edu.infrastructure.repository;

import com.ai.edu.domain.shared.service.RedisService;
import com.ai.edu.domain.user.model.valueobject.VerificationCode;
import com.ai.edu.domain.user.repository.VerificationCodeRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

/**
 * 验证码仓储实现 - Redis 存储
 *
 * @author AI Edu Platform
 */
@Slf4j
@Repository
public class VerificationCodeRepositoryImpl implements VerificationCodeRepository {

    @Resource
    private RedisService redisService;

    @Override
    public void save(VerificationCode verificationCode) {
        String key = verificationCode.toStorageKey();
        redisService.set(key, verificationCode.getCode(), verificationCode.getExpireMinutes(), TimeUnit.MINUTES);
        log.debug("验证码已存储: key={}", key);
    }

    @Override
    public VerificationCode findByPhoneAndScene(String phone, String scene) {
        String key = buildKey(phone, scene);
        String code = redisService.get(key);
        if (code == null) {
            return null;
        }
        // 返回一个用于验证的 VerificationCode
        return VerificationCode.of(phone, scene, code);
    }

    @Override
    public boolean verifyAndDelete(String phone, String scene, String code) {
        String key = buildKey(phone, scene);
        String storedCode = redisService.get(key);
        if (storedCode != null && storedCode.equals(code)) {
            redisService.delete(key);
            return true;
        }
        return false;
    }

    @Override
    public void delete(String phone, String scene) {
        String key = buildKey(phone, scene);
        redisService.delete(key);
    }

    private String buildKey(String phone, String scene) {
        return "ai-edu:code:" + phone + ":" + scene;
    }
}