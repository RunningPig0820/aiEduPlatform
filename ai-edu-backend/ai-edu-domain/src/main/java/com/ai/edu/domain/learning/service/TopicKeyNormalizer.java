package com.ai.edu.domain.learning.service;

import java.text.Normalizer;

/**
 * 题型名归一化工具（纯函数，无框架依赖）。
 *
 * <p>题型空间无限且命名不规整（LLM 随手输出），自由文本直接作掌握度主键会裂成多行。
 * 归一化收敛到稳定 {@code topic_key}：全角→半角、空白折叠、去末尾标点。
 * 例："鸡 兔 同 笼" / 全角写法 / 带末尾标点 → 同一 key「鸡兔同笼」。
 *
 * <p>注意：SHALL NOT 剥离「问题/题型」等后缀——「相遇问题/追及问题/工程问题」里的
 * 「问题」是题型名的固有部分，剥离会丢语义（同义词聚类留大数据阶段，design Decision 17）。
 */
public final class TopicKeyNormalizer {

    private TopicKeyNormalizer() {
    }

    /**
     * 归一化题型 label → 稳定 topic_key。
     *
     * @param label 原始题型 label（可为 null）
     * @return 归一化 key；null 输入返回 null
     */
    public static String normalize(String label) {
        if (label == null) {
            return null;
        }
        // ① Unicode NFKC：全角→半角、兼容字符规范化
        String s = Normalizer.normalize(label, Normalizer.Form.NFKC);
        // ② trim + 折叠所有空白
        s = s.trim().replaceAll("\\s+", "");
        // ③ 去末尾标点
        s = stripTrailingPunct(s);
        return s;
    }

    private static String stripTrailingPunct(String s) {
        while (!s.isEmpty() && isTrailingPunct(s.charAt(s.length() - 1))) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static boolean isTrailingPunct(char c) {
        return "，。！？、；：,.!?;:（）()[]【】\"'“”‘’…—~·".indexOf(c) >= 0;
    }
}
