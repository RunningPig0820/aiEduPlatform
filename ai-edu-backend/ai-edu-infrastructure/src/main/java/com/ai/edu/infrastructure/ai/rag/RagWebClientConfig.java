package com.ai.edu.infrastructure.ai.rag;

import com.ai.edu.infrastructure.ai.LlmGatewayProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * RAG 助手 Python 引擎 WebClient 配置。
 *
 * <p>复用 llm-gateway 的 baseUrl + internalToken（同一 ai-edu-ai-service 内独立模块），
 * 宽容 ObjectMapper（FAIL_ON_UNKNOWN_PROPERTIES=false + JavaTimeModule）。RagAskRequest 经
 * {@code @JsonProperty} 序列化为 snake_case 调 Python；Python 返回 snake_case SSE，由应用层重建为 camelCase。
 *
 * <p><b>NON_NULL（2026-08-29 多模块联调修复）</b>：前端不传 {@code currentProject} 时，若序列化
 * {@code "current_project":null}，Pydantic {@code str} 字段显式 null 会 422 → 整轮 ask 失败（契约
 * "缺省 rag-system" 被破坏）。改 NON_NULL 省略 null 字段 → Python 兜底默认模块（rag-system）。
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(LlmGatewayProperties.class)
public class RagWebClientConfig {

    @Bean
    public WebClient ragWebClient(LlmGatewayProperties properties) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(clientDefaultCodecsConfigurer -> {
                    clientDefaultCodecsConfigurer.defaultCodecs()
                            .jackson2JsonDecoder(new Jackson2JsonDecoder(mapper));
                    clientDefaultCodecsConfigurer.defaultCodecs()
                            .jackson2JsonEncoder(new Jackson2JsonEncoder(mapper));
                })
                .build();

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) Duration.ofSeconds(5).toMillis())
                .responseTimeout(Duration.ofSeconds(60));

        log.info("Initializing rag WebClient with baseUrl: {}", properties.getBaseUrl());
        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("x-internal-token", properties.getInternalToken())
                .exchangeStrategies(strategies)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
