package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.domain.learning.model.contract.SubjectClassifyRequest;
import com.ai.edu.domain.learning.model.contract.SubjectClassifyResult;
import com.ai.edu.domain.learning.service.SubjectClassifyPort;
import com.ai.edu.domain.learning.service.TutoringConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * 学科分类 Python 桥（WebClient，复用 {@link TutoringLlmClient} / {@link TopicVectorClient} 模式）。
 *
 * <p>subject-classify stateless 端点（Python 本期交付）：decide 之前判定题目学科，
 * 支持文本 + 图片（image_url 走多模态）。模型与 decide/understand 统一（doubao-seed-2-0-mini-260428）。
 *
 * <p><b>失败语义（design Decision 2 / Risks）</b>：绝不抛异常给调用方——
 * 异常/超时/空响应 → 返回空 subject（{@link SubjectClassifyResult#isEmpty()}），
 * 由编排层按 math 放行（宁可漏拦非数学题，不误拦数学题）。与
 * {@link TutoringLlmClient#understandQuestion}（识别失败返回空）同一降级哲学。
 */
@Slf4j
@Repository
public class SubjectClassifyClient implements SubjectClassifyPort {

    @Resource
    private WebClient tutoringWebClient;

    @Resource
    private TutoringConfig tutoringConfig;

    @Override
    public SubjectClassifyResult classify(SubjectClassifyRequest request) {
        log.info("[tutor-subject] subject-classify 调用, hasContent={}, hasImage={}",
                request.getContent() != null && !request.getContent().isBlank(),
                request.getImageUrl() != null && !request.getImageUrl().isBlank());
        try {
            SubjectClassifyResult resp = Mono.defer(() -> tutoringWebClient.post()
                    .uri(config().subjectClassifyPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(SubjectClassifyResult.class))
                    .retry(config().subjectClassifyRetry())
                    .block(config().subjectClassifyTimeout());
            if (resp == null || resp.isEmpty()) {
                return SubjectClassifyResult.builder().build();   // 空响应 → 空 subject（降级放行）
            }
            return resp;
        } catch (Exception e) {
            log.error("[tutor-subject] subject-classify 调用失败（降级空，编排层按 math 放行）: {}", e.getMessage(), e);
            return SubjectClassifyResult.builder().build();        // 绝不抛异常 → 空 subject
        }
    }

    /** 答疑配置（未注入时回退默认值，保持测试/默认行为一致）。 */
    private TutoringConfig config() {
        return tutoringConfig == null ? TutoringConfig.defaults() : tutoringConfig;
    }
}
