package com.ai.edu.infrastructure.config;

import com.ai.edu.domain.learning.service.TutoringConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 答疑配置（{@code ai-edu.tutoring}）——护栏规则数字 / 超时 / 端点 / OCR 开关。
 *
 * <p>实现域端口 {@link TutoringConfig}；application 层（护栏/编排）与
 * infrastructure 层（Python 客户端）均经该端口读取，避免 hardcode。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai-edu.tutoring")
public class TutoringProperties implements TutoringConfig {

    /** 轮次/答案护栏数字 */
    private Guardrail guardrail = new Guardrail();

    /** 会话创建频率限制 */
    private CreateLimit createLimit = new CreateLimit();

    /** Python 调用超时 */
    private Timeout timeout = new Timeout();

    /** Python decide/recognize 失败重试次数（generate 流式不可重试） */
    private int agentRetry = 1;

    /** 拍题 OCR 开关 */
    private Ocr ocr = new Ocr();

    /** Python 答疑端点路径 */
    private Python python = new Python();

    @Override
    public int roundLimit() {
        return guardrail.getRoundLimit();
    }

    @Override
    public int answerRequestLimit() {
        return guardrail.getAnswerRequestLimit();
    }

    @Override
    public int createLimit() {
        return createLimit.getLimit();
    }

    @Override
    public int createWindowMinutes() {
        return createLimit.getWindowMinutes();
    }

    @Override
    public int agentRetry() {
        return agentRetry;
    }

    @Override
    public Duration decideTimeout() {
        return timeout.getDecide();
    }

    @Override
    public Duration generateTimeout() {
        return timeout.getGenerate();
    }

    @Override
    public Duration ocrTimeout() {
        return timeout.getOcr();
    }

    @Override
    public boolean ocrEnabled() {
        return ocr.isEnabled();
    }

    @Override
    public String decidePath() {
        return python.getDecidePath();
    }

    @Override
    public String generatePath() {
        return python.getGeneratePath();
    }

    @Override
    public String ocrPath() {
        return python.getOcrPath();
    }

    @Data
    public static class Guardrail {
        /** 轮次上限（≤ 领域硬上限 20） */
        private int roundLimit = 20;
        /** 要答案次数上限 */
        private int answerRequestLimit = 2;
    }

    @Data
    public static class CreateLimit {
        /** 窗口内创建上限 */
        private int limit = 3;
        /** 窗口分钟数 */
        private int windowMinutes = 5;
    }

    @Data
    public static class Timeout {
        private Duration decide = Duration.ofSeconds(15);
        private Duration generate = Duration.ofSeconds(60);
        private Duration ocr = Duration.ofSeconds(30);
    }

    @Data
    public static class Ocr {
        /** 拍照识别开关（关闭时前端隐藏拍照入口，仅手打/粘贴） */
        private boolean enabled = true;
    }

    @Data
    public static class Python {
        private String decidePath = "/api/tutoring/decide";
        private String generatePath = "/api/tutoring/generate";
        private String ocrPath = "/api/ocr/recognize";
    }
}
