package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.infrastructure.ai.LlmGatewayProperties;
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
 * 答疑 Python agent WebClient 配置。
 *
 * <p>复用 llm-gateway 的 baseUrl + internalToken（同一 ai-edu-ai-service 内的独立模块），
 * 但使用<b>宽容 ObjectMapper</b>（FAIL_ON_UNKNOWN_PROPERTIES=false + JavaTimeModule）——
 * Python decide 响应含可选调试字段 {@code reason}，Java 不建模需容忍未知字段。
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(LlmGatewayProperties.class)
public class TutoringWebClientConfig {

    @Bean
    public WebClient tutoringWebClient(LlmGatewayProperties properties) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
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

        log.info("Initializing tutoring WebClient with baseUrl: {}", properties.getBaseUrl());
        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("x-internal-token", properties.getInternalToken())
                .exchangeStrategies(strategies)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
