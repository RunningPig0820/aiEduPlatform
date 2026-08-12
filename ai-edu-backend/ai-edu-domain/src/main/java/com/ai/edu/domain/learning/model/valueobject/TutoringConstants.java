package com.ai.edu.domain.learning.model.valueobject;

import java.time.Duration;

/**
 * 答疑领域常量——护栏规则数字 + Python 调用超时。
 *
 * <p>规则数字（轮次/要答案次数/频率）后续可迁配置中心/后台运营
 * （设计：审批规则数字可页面/配置运营控制，不被 prompt 攻击影响）。
 */
public final class TutoringConstants {

    private TutoringConstants() {
    }

    /** 单会话轮次上限（round_count ≤ 20，达顶强制收尾 ROUND_LIMIT） */
    public static final int SESSION_ROUND_LIMIT = 20;

    /** 请求答案次数上限（第 1 次思路 approach / 第 2 次答案 reveal） */
    public static final int ANSWER_REQUEST_LIMIT = 2;

    /** 会话创建频率限制：窗口内最多创建数（超限拒绝，提示先完成当前答疑） */
    public static final int SESSION_CREATE_LIMIT = 3;

    /** 会话创建频率窗口（分钟） */
    public static final int SESSION_CREATE_WINDOW_MINUTES = 5;

    /** Python decide/recognize 失败重试次数（generate 流式不可重试） */
    public static final int AGENT_RETRY = 1;

    /** decide 调用超时（流式；2026-08 关思考 + 换 mini 后实测 ~1.5s，10s 防挂起/边缘误超时） */
    public static final Duration DECIDE_TIMEOUT = Duration.ofSeconds(10);

    /** generate 调用超时（流式正文） */
    public static final Duration GENERATE_TIMEOUT = Duration.ofSeconds(60);

    /** OCR 调用超时 */
    public static final Duration OCR_TIMEOUT = Duration.ofSeconds(30);

    /** transcript 签名 URL 有效期（分钟，对外读时经 generatePresignedUrl 现生成，避免死链接） */
    public static final int TRANSCRIPT_URL_EXPIRE_MINUTES = 30;
}
