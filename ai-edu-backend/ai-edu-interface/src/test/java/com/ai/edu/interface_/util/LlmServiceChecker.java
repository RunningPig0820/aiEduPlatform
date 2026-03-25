package com.ai.edu.interface_.util;

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
}