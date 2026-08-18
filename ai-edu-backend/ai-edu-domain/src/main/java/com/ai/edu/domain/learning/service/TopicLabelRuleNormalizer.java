package com.ai.edu.domain.learning.service;

import java.util.Collection;

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
 *   <li><b>近字变体</b> {@link #nearestByEditDistance(String, Collection)}：编辑距离 ≤1（错别字/漏字级别）
 *       归并到池中 canonical；距离 2（如「鸡兔同笼问题」加后缀）不归并——留给向量。</li>
 * </ul>
 */
public final class TopicLabelRuleNormalizer {

    private TopicLabelRuleNormalizer() {
    }

    /** 高置信动作前缀（「解X→X」「求X→X」）。 */
    private static final String[] STRIP_PREFIXES = {"解", "求"};

    /** 剥离前缀后剩余最小长度（防「求导→导」碎词误并）。 */
    private static final int MIN_REMAINDER_LEN = 2;

    /** 近字变体编辑距离上限（≤1 = 1 字符差异：替换/插入/删除单个字符）。 */
    private static final int MAX_EDIT_DISTANCE = 1;

    /**
     * 字符规则归一（无池）：复用 {@link TopicKeyNormalizer} + 前缀剥离。返回规整后的题型名。
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

    /**
     * 近字变体归并（带池）：编辑距离 ≤1 的 canonical 返回之，否则 null。
     *
     * <p>编辑距离是「和谁比」，必须有池（已确立 canonical 集合，如题型库 topic_label）——
     * 2.5 编排从题库查出池传入；池空（首题零锚点）→ null，走向量建锚。
     *
     * @param label 待归并题型名（已 normalize 更佳，可为 null）
     * @param pool  已确立 canonical 名集合（可为 null/空）
     * @return 编辑距离 ≤1 的池成员（取最相似）；无则 null
     */
    public static String nearestByEditDistance(String label, Collection<String> pool) {
        if (label == null || pool == null || pool.isEmpty()) {
            return null;
        }
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        for (String candidate : pool) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            int dist = levenshtein(label, candidate);
            if (dist <= MAX_EDIT_DISTANCE && dist < bestDist) {
                best = candidate;
                bestDist = dist;
                if (dist == 0) {
                    return candidate; // 完全相等，最相似
                }
            }
        }
        return best;
    }

    /** Levenshtein 编辑距离（按字符，中文单字符一个 code unit）。 */
    private static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }
}
