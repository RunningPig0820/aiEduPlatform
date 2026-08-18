package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.common.exception.TutoringAgentException;
import com.ai.edu.domain.learning.model.contract.TopicVectorNeighbor;
import com.ai.edu.domain.learning.model.contract.TopicVectorPutRequest;
import com.ai.edu.domain.learning.service.TutoringConfig;
import com.ai.edu.domain.learning.service.TopicVectorStore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * 题型名向量 Python 桥（WebClient，复用 {@link TutoringLlmClient} 模式，tasks 2.3.3）。
 *
 * <p>本期只存<b>题型名向量</b>：{@code vector_type} 恒为 {@code "topic"}（Python 契约必填路由键、
 * 无缺省，在桥内部收口，调用方无法传错）。Java 不碰 embedding API / COS SDK（无 Java SDK）。
 *
 * <p>契约要点（python-integration.md，Python 已交付 b7159c5）：
 * <ul>
 *   <li>put：{key, text, vector_type, metadata} → {ok, key}，key 相同覆盖（upsert）</li>
 *   <li>query：{text, top_k, vector_type} → <b>{@code vectors}:[{key, metadata, distance}]</b>（非 hits）</li>
 *   <li>distance 越小越相似（cosine，self ≈ 0）；put 后 ~10s 异步生效，立即 query 会 miss</li>
 *   <li>未知 vector_type / 端点异常 → 抛 {@link TutoringAgentException}，调用方降级（回退字符规则 + 原样落库）</li>
 * </ul>
 */
@Slf4j
@Repository
public class TopicVectorClient implements TopicVectorStore {

    /** 本期唯一向量类型路由键：题型名向量索引（Python 契约必填，无缺省）。 */
    private static final String VECTOR_TYPE_TOPIC = "topic";

    @Resource
    private WebClient tutoringWebClient;

    @Resource
    private TutoringConfig tutoringConfig;

    @Override
    public String putVector(TopicVectorPutRequest request) {
        log.info("[tutor-vector] put 题型名向量, key={}, topic={}", request.getKey(), request.getText());
        try {
            // vector_type 由桥强制 "topic"（收口路由键），metadata 原样透传
            TopicVectorPutRequest body = TopicVectorPutRequest.builder()
                    .key(request.getKey())
                    .text(request.getText())
                    .vectorType(VECTOR_TYPE_TOPIC)
                    .metadata(request.getMetadata())
                    .build();
            VectorPutResponse resp = Mono.defer(() -> tutoringWebClient.post()
                    .uri(config().vectorPutPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(VectorPutResponse.class))
                    .retry(config().agentRetry())
                    .block(config().vectorPutTimeout());
            return resp == null || resp.getKey() == null ? request.getKey() : resp.getKey();
        } catch (Exception e) {
            log.error("[tutor-vector] put 调用失败（调用方降级回退字符规则）: {}", e.getMessage(), e);
            throw new TutoringAgentException("题型名向量存储服务暂不可用", e);
        }
    }

    @Override
    public Optional<TopicVectorNeighbor> queryNearestTop1(String text) {
        log.info("[tutor-vector] query 题型名最近邻, text={}", text);
        try {
            VectorQueryBody body = new VectorQueryBody();
            body.setText(text);
            body.setTopK(1);
            body.setVectorType(VECTOR_TYPE_TOPIC);
            VectorQueryResponse resp = Mono.defer(() -> tutoringWebClient.post()
                    .uri(config().vectorQueryPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(VectorQueryResponse.class))
                    .retry(config().agentRetry())
                    .block(config().vectorQueryTimeout());
            if (resp == null || resp.getVectors() == null || resp.getVectors().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(resp.getVectors().get(0));
        } catch (Exception e) {
            log.error("[tutor-vector] query 调用失败（调用方降级建新 canonical）: {}", e.getMessage(), e);
            throw new TutoringAgentException("题型名向量检索服务暂不可用", e);
        }
    }

    /** 答疑配置（未注入时回退默认值，保持测试/默认行为一致）。 */
    private TutoringConfig config() {
        return tutoringConfig == null ? TutoringConfig.defaults() : tutoringConfig;
    }

    /** put 响应 {ok, key}。 */
    @Data
    public static class VectorPutResponse {
        private boolean ok;
        private String key;
    }

    /** query 请求（snake_case：top_k / vector_type 必填，Python 契约）。 */
    @Data
    public static class VectorQueryBody {
        private String text;
        @JsonProperty("top_k")
        private int topK;
        @JsonProperty("vector_type")
        private String vectorType;
    }

    /** query 响应 {vectors:[{key, metadata, distance}]}——对齐 COS query_vectors 返回（非 hits）。 */
    @Data
    public static class VectorQueryResponse {
        private List<TopicVectorNeighbor> vectors;
    }
}
