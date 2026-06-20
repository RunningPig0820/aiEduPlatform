package com.ai.edu.common.util;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;

/**
 * AES 加解密工具类
 *
 * 用于敏感数据（如身份证号）的加密存储和脱敏展示。
 * 加密逻辑放在基础设施层调用，领域层不感知加密细节。
 *
 * 密钥通过 application.yml 的 app.encrypt.aes-key 配置，支持环境变量覆盖。
 */
public final class EncryptUtil {

    private static volatile AES aes;

    private EncryptUtil() {}

    /**
     * 初始化 AES 实例（由 Spring 容器启动时调用一次）
     *
     * @param aesKey AES 密钥（16/24/32 字节）
     */
    public static void init(String aesKey) {
        if (aes == null) {
            synchronized (EncryptUtil.class) {
                if (aes == null) {
                    aes = SecureUtil.aes(aesKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
            }
        }
    }

    /**
     * AES 加密，返回 Base64 编码的密文
     *
     * @param plainText 明文
     * @return Base64 密文
     */
    public static String encrypt(String plainText) {
        ensureInitialized();
        return aes.encryptBase64(plainText);
    }

    /**
     * AES 解密
     *
     * @param cipherText Base64 密文
     * @return 明文
     */
    public static String decrypt(String cipherText) {
        ensureInitialized();
        return aes.decryptStr(cipherText);
    }

    /**
     * 身份证号脱敏
     * 保留前 6 位和后 6 位，中间用 * 替换
     *
     * @param idCard 完整身份证号（明文）
     * @return 脱敏后的身份证号，如 "110101****011234"
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        int len = idCard.length();
        int maskLen = len - 12; // 去掉前6后6
        if (maskLen <= 0) {
            return idCard;
        }
        return idCard.substring(0, 6) + "*".repeat(maskLen) + idCard.substring(len - 6);
    }

    private static void ensureInitialized() {
        if (aes == null) {
            throw new IllegalStateException("EncryptUtil is not initialized. Please configure app.encrypt.aes-key.");
        }
    }
}
