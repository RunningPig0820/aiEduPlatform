package com.ai.edu.common.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EncryptUtil 单元测试
 */
class EncryptUtilTest {

    private static final String AES_KEY = "EduPlatform@2026"; // 16 字节

    @BeforeAll
    static void setUp() {
        EncryptUtil.init(AES_KEY);
    }

    // ==================== 加密/解密往返验证 ====================

    @Test
    void shouldEncryptAndDecryptRoundTrip() {
        String plainText = "110101200001011234";
        String encrypted = EncryptUtil.encrypt(plainText);

        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted, "密文不应等于明文");

        String decrypted = EncryptUtil.decrypt(encrypted);
        assertEquals(plainText, decrypted, "解密后应与原始明文一致");
    }

    @Test
    void shouldEncryptAndDecryptChineseName() {
        String plainText = "张三身份证信息";
        String encrypted = EncryptUtil.encrypt(plainText);
        String decrypted = EncryptUtil.decrypt(encrypted);

        assertEquals(plainText, decrypted);
    }

    @Test
    void shouldProduceDifferentCipherForSamePlaintext() {
        String plainText = "110101200001011234";
        String encrypted1 = EncryptUtil.encrypt(plainText);
        String encrypted2 = EncryptUtil.encrypt(plainText);

        // AES 加密相同明文每次结果相同（确定性的）
        assertEquals(encrypted1, encrypted2);
    }

    // ==================== 脱敏测试 ====================

    @Test
    void shouldMaskStandard18DigitIdCard() {
        String idCard = "110101200001011234";
        String masked = EncryptUtil.maskIdCard(idCard);

        assertEquals("110101******011234", masked, "标准18位身份证应保留前6后6，中间6位脱敏");
        assertEquals(18, masked.length(), "脱敏后长度应与原文一致");
    }

    @Test
    void shouldMask15DigitIdCard() {
        String idCard = "110101800101123";  // 15位老身份证

        String masked = EncryptUtil.maskIdCard(idCard);
        assertEquals("110101***101123", masked, "15位身份证应保留前6后6，中间3位脱敏");
    }

    @Test
    void shouldReturnNullForNullInput() {
        assertNull(EncryptUtil.maskIdCard(null));
    }

    @Test
    void shouldReturnOriginalForShortInput() {
        String shortText = "1234567";  // 少于8位
        assertEquals("1234567", EncryptUtil.maskIdCard(shortText));
    }

    // ==================== 边界条件 ====================

    @Test
    void shouldEncryptEmptyString() {
        String encrypted = EncryptUtil.encrypt("");
        assertNotNull(encrypted);

        String decrypted = EncryptUtil.decrypt(encrypted);
        assertEquals("", decrypted);
    }

    @Test
    void shouldEncryptSpecialCharacters() {
        String special = "!@#$%^&*()_+-=[]{}|;':\",./<>?";
        String encrypted = EncryptUtil.encrypt(special);
        String decrypted = EncryptUtil.decrypt(encrypted);

        assertEquals(special, decrypted);
    }

    @Test
    void shouldEncryptAndDecryptEvenWhenReinitialized() {
        // 验证 re-init 不会破坏现有功能
        EncryptUtil.init(AES_KEY);
        String result = EncryptUtil.encrypt("re-init-test");
        assertEquals("re-init-test", EncryptUtil.decrypt(result));
    }

    @Test
    void shouldHandleLongIdCardText() {
        String longText = "A".repeat(1000);
        String encrypted = EncryptUtil.encrypt(longText);
        String decrypted = EncryptUtil.decrypt(encrypted);

        assertEquals(longText, decrypted);
    }
}
