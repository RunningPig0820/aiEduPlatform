package com.ai.edu.domain.learning.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 题型名归一化领域单测（test.md NORM-001/002）。
 */
class TopicKeyNormalizerTest {

    @Test
    @DisplayName("null 输入返回 null")
    void nullInput_returnsNull() {
        assertNull(TopicKeyNormalizer.normalize(null));
    }

    @Test
    @DisplayName("trim + 空白折叠 → 同一 key")
    void collapsesWhitespace() {
        assertEquals("鸡兔同笼", TopicKeyNormalizer.normalize("  鸡 兔 同 笼  "));
    }

    @Test
    @DisplayName("保留题型名的固有「问题」后缀（相遇问题 ≠ 相遇）")
    void preservesProblemSuffix() {
        assertEquals("相遇问题", TopicKeyNormalizer.normalize("相遇问题"));
        assertEquals("追及问题", TopicKeyNormalizer.normalize("追及问题"));
    }

    @Test
    @DisplayName("去末尾标点")
    void stripsTrailingPunct() {
        assertEquals("鸡兔同笼", TopicKeyNormalizer.normalize("鸡兔同笼？"));
        assertEquals("鸡兔同笼", TopicKeyNormalizer.normalize("鸡兔同笼！"));
    }

    @Test
    @DisplayName("全角 → 半角（NFKC）")
    void fullWidthToHalfWidth() {
        assertEquals("abc123", TopicKeyNormalizer.normalize("ａｂｃ１２３"));
    }

    @Test
    @DisplayName("不同写法归一化到同一 key")
    void differentWritings_sameKey() {
        assertEquals(TopicKeyNormalizer.normalize("鸡兔同笼"),
                TopicKeyNormalizer.normalize("鸡 兔 同 笼"));
    }
}
