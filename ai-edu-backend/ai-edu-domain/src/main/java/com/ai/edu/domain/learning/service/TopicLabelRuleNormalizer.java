package com.ai.edu.domain.learning.service;

/**
 * 字符级规则题型名归一（tasks 2.4.1，纯函数无框架依赖）——聚集编排第一道拦截。
 *
 * <p>LLM 猜的题型名会飘（弱标注），高频<b>确定性</b>变体在本地字符串级归并，
 * 省 embedding 调用（快/便宜/确定性）。只做<b>高置信</b>变换，低置信一律放行给向量层：
 * <ul>
 *   <li><b>复用 {@link TopicKeyNormalizer}</b>：全角→半角、空白折叠、去末尾标点。</li>
 *   <li><b>前缀剥离</b>「解X→X」「求X→X」：动作前缀高置信，剥离后剩余过短（&lt;{@link #MIN_REMAINDER_LEN}）
 *       不剥（防「求导→导」碎词误并）。</li>
 *   <li><b>后缀保留</b>：SHALL NOT 剥离「问题/题型」——是题型固有部分（如「相遇问题」），
 *       剥了丢语义，留给向量层（同型 distance ~0.077 抓得住）。</li>
 *   <li><b>近字变体归并已移除</b>（拍板）：编辑距离 ≤1 无法区分「一字之差不同题型」（一元一次/一元二次，
 *       distance 1 与错别字同）与错别字——近字/错别字归并全交给题型名向量层（语义最近邻正确区分/归并）。</li>
 * </ul>
 */
public final class TopicLabelRuleNormalizer {

    private TopicLabelRuleNormalizer() {
    }

    /** 高置信动作前缀（「解X→X」「求X→X」）。 */
    private static final String[] STRIP_PREFIXES = {"解", "求"};

    /** 剥离前缀后剩余最小长度（防「求导→导」碎词误并）。 */
    private static final int MIN_REMAINDER_LEN = 2;

    /**
     * 字符规则归一：复用 {@link TopicKeyNormalizer} + 前缀剥离。返回规整后的题型名。
     *
     * @param label 原始题型 label（可为 null）
     * @return 规整名；null 输入返回 null
     */
    public static String normalize(String label) {
        if (label == null) {
            return null;
        }
        // ① 复用 TopicKeyNormalizer：全角→半角、空白折叠、去末尾标点
        String s = TopicKeyNormalizer.normalize(label);
        if (s == null) {
            return null;
        }
        // ② 高置信前缀剥离（保守：剥离后剩余过短不剥，宁可不并不误并）
        for (String prefix : STRIP_PREFIXES) {
            if (s.startsWith(prefix) && s.length() >= prefix.length() + MIN_REMAINDER_LEN) {
                s = s.substring(prefix.length());
                break;
            }
        }
        return s;
    }
}
