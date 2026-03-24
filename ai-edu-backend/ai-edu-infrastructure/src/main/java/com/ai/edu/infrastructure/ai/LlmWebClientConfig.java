package com.ai.edu.infrastructure.ai;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * LLM Gateway WebClient 配置
 *
 * @author AI Edu Platform
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(LlmGatewayProperties.class)
public class LlmWebClientConfig {

    /**
     * 配置 LLM Gateway WebClient Bean
     *
     * @param properties LLM Gateway 配置属性
     * @return 配置好的 WebClient 实例
     */
    @Bean
    public WebClient llmWebClient(LlmGatewayProperties properties) {
        log.info("Initializing LLM Gateway WebClient with baseUrl: {}", properties.getBaseUrl());

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) properties.getConnectTimeout().toMillis())
                .responseTimeout(properties.getReadTimeout())
                .doOnConnected(conn -> log.debug("LLM Gateway connection established"))
                .doOnDisconnected(conn -> log.debug("LLM Gateway connection closed"));

        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("x-internal-token", properties.getInternalToken())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}