package com.ai.edu.domain.learning.service;

import com.ai.edu.domain.learning.model.valueobject.TutoringConstants;

import java.time.Duration;

/**
 * 答疑配置端口——护栏规则数字 / Python 调用超时与端点 / OCR 开关。
 *
 * <p>规则数字（轮次/要答案/频率）与超时/端点从 hardcode 迁到配置
 * （application.yml {@code ai-edu.tutoring}，运营可调，见任务 11.1）。
 * 实现位于 Infrastructure 层（{@code TutoringProperties}，@ConfigurationProperties）。
 *
 * <p><b>领域硬上限</b>：轮次硬天花板仍由 {@link TutoringConstants#SESSION_ROUND_LIMIT}（20）保证
 * （{@code TutoringSession.recordRound()} 触发），配置 {@code roundLimit} 应 ≤ 20。
 */
public interface TutoringConfig {

    /** 轮次护栏上限（引导类 hint/approach 达限强制收尾 ROUND_LIMIT）。 */
    int roundLimit();

    /** 请求答案次数上限（第 1 次思路 / 第 2 次答案）。 */
    int answerRequestLimit();

    /** 会话创建频率：窗口内上限。 */
    int createLimit();

    /** 会话创建频率：窗口分钟数。 */
    int createWindowMinutes();

    /** Python decide/recognize 失败重试次数（generate 流式不可重试）。 */
    int agentRetry();

    /** decide 调用超时（非流式，快模型）。 */
    Duration decideTimeout();

    /** generate 调用超时（流式正文）。 */
    Duration generateTimeout();

    /** OCR 调用超时。 */
    Duration ocrTimeout();

    /** 拍题 OCR 开关（关闭时前端隐藏拍照入口，仅手打/粘贴）。 */
    boolean ocrEnabled();

    /** Python decide 端点路径。 */
    String decidePath();

    /** Python generate 端点路径。 */
    String generatePath();

    /** Python OCR 端点路径。 */
    String ocrPath();

    /** 图片题目理解调用超时（视觉模型看图，30s 兜底）。 */
    Duration questionUnderstandTimeout();

    /** Python 图片题目理解端点路径（POST /api/tutoring/question-understand，视觉模型看图）。 */
    String questionUnderstandPath();

    /** 题型名向量 put 调用超时（embedding + 写入向量桶，30s 兜底）。 */
    Duration vectorPutTimeout();

    /** Python 题型名向量 put 端点路径（POST /api/tutoring/vector/put）。 */
    String vectorPutPath();

    /** 题型名向量 query 调用超时（embedding + 检索，30s 兜底）。 */
    Duration vectorQueryTimeout();

    /** Python 题型名向量 query 端点路径（POST /api/tutoring/vector/query）。 */
    String vectorQueryPath();

    /** 默认配置（回退值，与 TutoringConstants 对齐；测试/未注入时使用）。 */
    static TutoringConfig defaults() {
        return new TutoringConfig() {
            @Override
            public int roundLimit() {
                return TutoringConstants.SESSION_ROUND_LIMIT;
            }

            @Override
            public int answerRequestLimit() {
                return TutoringConstants.ANSWER_REQUEST_LIMIT;
            }

            @Override
            public int createLimit() {
                return TutoringConstants.SESSION_CREATE_LIMIT;
            }

            @Override
            public int createWindowMinutes() {
                return TutoringConstants.SESSION_CREATE_WINDOW_MINUTES;
            }

            @Override
            public int agentRetry() {
                return TutoringConstants.AGENT_RETRY;
            }

            @Override
            public Duration decideTimeout() {
                return TutoringConstants.DECIDE_TIMEOUT;
            }

            @Override
            public Duration generateTimeout() {
                return TutoringConstants.GENERATE_TIMEOUT;
            }

            @Override
            public Duration ocrTimeout() {
                return TutoringConstants.OCR_TIMEOUT;
            }

            @Override
            public boolean ocrEnabled() {
                return true;
            }

            @Override
            public String decidePath() {
                return "/api/tutoring/decide";
            }

            @Override
            public String generatePath() {
                return "/api/tutoring/generate";
            }

            @Override
            public String ocrPath() {
                return "/api/ocr/recognize";
            }

            @Override
            public Duration questionUnderstandTimeout() {
                return Duration.ofSeconds(30);
            }

            @Override
            public String questionUnderstandPath() {
                return "/api/tutoring/question-understand";
            }

            @Override
            public Duration vectorPutTimeout() {
                return TutoringConstants.VECTOR_PUT_TIMEOUT;
            }

            @Override
            public String vectorPutPath() {
                return "/api/tutoring/vector/put";
            }

            @Override
            public Duration vectorQueryTimeout() {
                return TutoringConstants.VECTOR_QUERY_TIMEOUT;
            }

            @Override
            public String vectorQueryPath() {
                return "/api/tutoring/vector/query";
            }
        };
    }
}
