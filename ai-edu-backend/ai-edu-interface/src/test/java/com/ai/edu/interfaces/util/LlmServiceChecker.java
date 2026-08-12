package com.ai.edu.interfaces.util;

import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * LLM 服务可用性检测工具
 */
public class LlmServiceChecker {

    /**
     * 检测 LLM 服务是否可用
     *
     * @param baseUrl LLM 服务地址
     * @param timeout 超时时间（毫秒）
     * @return true 表示服务可用
     */
    public static boolean isAvailable(String baseUrl, String token, long timeout) {
        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("x-internal-token", token)
                    .build();

            webClient.get()
                    .uri("/api/llm/allowed-models")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检测 LLM 服务是否可用（默认 2 秒超时）
     *
     * @param baseUrl LLM 服务地址
     * @return true 表示服务可用
     */
    public static boolean isAvailable(String baseUrl, String token) {
        return isAvailable(baseUrl, token, 2000);
    }

    /**
     * 检测 LLM 服务场景列表端点是否可用
     *
     * 独立于 isAvailable 探测 /api/llm/scenes：Python 服务（ai-edu-ai-service）可能已实现
     * 主链路（chat / models / allowed-models）但尚未实现场景端点，此时 /scenes 返回 404。
     * 场景测试应等 Python 侧实现后再启用，而非在无该端点时强跑失败。
     *
     * @param baseUrl LLM 服务地址
     * @param timeout 超时时间（毫秒）
     * @return true 表示场景端点可用
     */
    public static boolean isScenesAvailable(String baseUrl, String token, long timeout) {
        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("x-internal-token", token)
                    .build();

            webClient.get()
                    .uri("/api/llm/scenes")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}