package com.ai.edu.domain.learning.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 字符级规则 {@link TopicLabelRuleNormalizer} 测试（tasks 2.4.2，test.md NOR-001）。
 *
 * <p>规则层只做<b>高置信</b>确定性变体归并（省 embedding 调用）：
 * 前缀剥离「解X→X」「求X→X」（护栏：剥离后过短不剥）、复用 {@link TopicKeyNormalizer}
 * （全半角/空白/去标点）、近字变体编辑距离 ≤1（带池比对）；「问题」等后缀<b>不剥离</b>
 * （留给向量层，TopicKeyNormalizer 已注释 SHALL NOT）。
 */
class TopicLabelRuleNormalizerTest {

    // ---------- 前缀变体 ----------

    @Test
    @DisplayName("前缀变体：解X→X（NOR-001：解一元二次方程 → 一元二次方程，零 embedding 调用）")
    void stripsSolvePrefix() {
        assertEquals("一元二次方程", TopicLabelRuleNormalizer.normalize("解一元二次方程"));
    }

    @Test
    @DisplayName("前缀变体：求X→X")
    void stripsFindPrefix() {
        assertEquals("鸡兔同笼", TopicLabelRuleNormalizer.normalize("求鸡兔同笼"));
    }

    @Test
    @DisplayName("前缀变体：复用 TopicKeyNormalizer——全角/空白/标点先归一再剥离")
    void reusesTopicKeyNormalizer() {
        assertEquals("一元二次方程", TopicLabelRuleNormalizer.normalize("解 一元二次方程。"));
    }

    @Test
    @DisplayName("前缀护栏：剥离后过短不剥离（求导 → 求导，防碎词）")
    void shortRemainder_notStripped() {
        assertEquals("求导", TopicLabelRuleNormalizer.normalize("求导"));
    }

    // ---------- 后缀保留 ----------

    @Test
    @DisplayName("后缀保留：「问题」不剥离（相遇问题/鸡兔同笼问题保持原样，留给向量层）")
    void keepsProblemSuffix() {
        assertEquals("相遇问题", TopicLabelRuleNormalizer.normalize("相遇问题"));
        assertEquals("鸡兔同笼问题", TopicLabelRuleNormalizer.normalize("鸡兔同笼问题"));
    }

    // 近字变体归并（编辑距离 ≤1）已移除（拍板：无法区分「一元一次/一元二次」一字之差与错别字，
    // 近字/错别字归并全交给向量层）——见 TopicLabelRuleNormalizer 类注释与 TopicLabelAggregationService
}
