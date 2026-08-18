package com.ai.edu.application.service.learning;

import com.ai.edu.domain.learning.model.contract.TopicVectorMetadata;
import com.ai.edu.domain.learning.model.contract.TopicVectorNeighbor;
import com.ai.edu.domain.learning.model.contract.TopicVectorPutRequest;
import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.model.entity.QuestionTypeAlias;
import com.ai.edu.domain.learning.model.valueobject.QuestionTypeStatus;
import com.ai.edu.domain.learning.repository.QuestionTypeAliasRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import com.ai.edu.domain.learning.service.TopicLabelRuleNormalizer;
import com.ai.edu.domain.learning.service.TopicVectorStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 题型聚集编排（tasks 2.5）——「题型名统一到 canonical」的核心链路。
 *
 * <p>输入 LLM 猜的原始题型名 → 输出 canonical 规范名（所有下游统一用它：题目表
 * canonical_label / 掌握表 topic_key / analyze 返回 topicLabel，不裂行）：
 * <ol>
 *   <li><b>字符规则</b> {@link TopicLabelRuleNormalizer}：确定性变体本地归一（全半角/空白/解求前缀），
 *       近字变体（编辑距离 ≤1）对题型库池归并——命中免 embedding 调用。</li>
 *   <li><b>题型名向量最近邻</b>（单信号，Python 桥）：distance ≤ {@link #mergeDistance} → 归并到命中
 *       canonical（写别名表）；中阈值区间（{@code mergeDistance}~{@code arbitrateDistance}，候选 LLM 仲裁——
 *       本期保守放行不误并）与异型（≥ {@code arbitrateDistance}）一样<b>建新</b>，宁可拆不误并。</li>
 *   <li><b>首题建锚</b>（零锚点）：无近邻 → 题型库建 canonical（CANDIDATE，幂等 upsert）+ 题型名向量入库
 *       （metadata.canonical_label，供后续题检索）。</li>
 *   <li><b>失败兜底</b>：向量库不可用 → 回退字符规则结果，不阻塞（原样落库由调用方）。</li>
 * </ol>
 *
 * <p>canonical 权威存 MySQL（题型库表 + 掌握表 key），COS 只存「canonical 名→向量」检索镜像。
 */
@Slf4j
@Service
public class TopicLabelAggregationService {

    /** 归并阈值（cosine distance ≤ 0.2 归并；spike：同型 ~0.077 / 异型 ≥0.33，保守宁可拆不误并）。 */
    @Value("${ai-edu.kp.topic-merge-distance:0.2}")
    private double mergeDistance = 0.2;

    /** 中阈值仲裁上界（0.2 < distance < 0.3 → 候选 LLM 仲裁；本期保守放行不误并，仲裁为扩展点）。 */
    @Value("${ai-edu.kp.topic-arbitrate-distance:0.3}")
    private double arbitrateDistance = 0.3;

    @Resource
    private TopicVectorStore topicVectorStore;

    @Resource
    private QuestionTypeRepository questionTypeRepository;

    @Resource
    private QuestionTypeAliasRepository questionTypeAliasRepository;

    /**
     * 题型聚集：原始题型名 → canonical 规范名。
     *
     * @param rawLabel  LLM 猜的原始题型名（可为 null/空，原样返回）
     * @param studentId 学生 ID（建锚向量 metadata 用；可为 null）
     * @return canonical 名（归并目标 / 建锚名 / 兜底字符规则归一结果）
     */
    public String aggregate(String rawLabel, Long studentId) {
        if (rawLabel == null || rawLabel.isBlank()) {
            return rawLabel;
        }
        // ① 字符规则归一（复用 Normalizer：全半角/空白/去标点 + 「解/求」前缀剥离）
        String normalized = TopicLabelRuleNormalizer.normalize(rawLabel);
        if (normalized == null || normalized.isBlank()) {
            return rawLabel;
        }

        // ② 字符规则近字归并（池 = 题型库全部 canonical，编辑距离 ≤1）——命中免 embedding 调用
        String poolHit = TopicLabelRuleNormalizer.nearestByEditDistance(normalized, canonicalPool());
        if (poolHit != null) {
            aliasToCanonical(normalized, poolHit);
            return poolHit;
        }

        // ③ 题型名向量最近邻（单信号）
        try {
            Optional<TopicVectorNeighbor> neighbor = topicVectorStore.queryNearestTop1(normalized);
            if (neighbor.isEmpty()) {
                // 无近邻（库空 / put 后 ~10s 异步未生效）→ 首题建锚（零锚点）
                return createAnchor(normalized, studentId);
            }
            TopicVectorNeighbor n = neighbor.get();
            double distance = n.getDistance() == null ? Double.MAX_VALUE : n.getDistance();
            if (distance <= mergeDistance) {
                // 高相似 → 归并到命中 canonical（metadata.canonical_label 为权威镜像）
                String canonical = n.getMetadata() == null ? null : n.getMetadata().getCanonicalLabel();
                if (canonical == null || canonical.isBlank()) {
                    return createAnchor(normalized, studentId);
                }
                aliasToCanonical(normalized, canonical);
                return canonical;
            }
            // distance > mergeDistance：中阈值区间（候选 LLM 仲裁，本期保守放行不误并）
            // 与异型（≥ arbitrateDistance）一样建新——宁可拆不误并（tasks 2.5.3）
            return createAnchor(normalized, studentId);
        } catch (Exception e) {
            // ④ 失败兜底：向量库不可用 → 回退字符规则结果，不阻塞（原样落库由调用方）
            log.warn("[topic-aggregate] 向量不可用，回退字符规则: {}", normalized, e);
            return normalized;
        }
    }

    /** 题型库 canonical 池（供近字变体编辑距离比对）。 */
    private List<String> canonicalPool() {
        return questionTypeRepository.findAll().stream()
                .map(QuestionType::getTopicLabel)
                .filter(t -> t != null && !t.isBlank())
                .toList();
    }

    /** 归并：写别名表（variant → canonical 题型，alias_label 唯一幂等）；variant 即 canonical 时跳过。 */
    private void aliasToCanonical(String variant, String canonical) {
        if (variant.equals(canonical)) {
            return;
        }
        QuestionType qt = questionTypeRepository.findByTopicLabel(canonical)
                .orElseGet(() -> questionTypeRepository.upsert(
                        QuestionType.create(canonical, QuestionTypeStatus.CANDIDATE, null)));
        questionTypeAliasRepository.upsert(QuestionTypeAlias.create(variant, qt.getId()));
        log.info("[topic-aggregate] 归并: {} → canonical[{}]", variant, canonical);
    }

    /** 首题建锚（零锚点）：题型库建 canonical（CANDIDATE，topic_label 唯一幂等）+ 题型名向量入库。 */
    private String createAnchor(String canonical, Long studentId) {
        QuestionType created = questionTypeRepository.upsert(
                QuestionType.create(canonical, QuestionTypeStatus.CANDIDATE, studentId));
        TopicVectorPutRequest request = TopicVectorPutRequest.builder()
                .key(anchorKey(canonical))
                .text(canonical)
                .metadata(TopicVectorMetadata.builder()
                        .studentId(String.valueOf(studentId))
                        .topicLabel(canonical)
                        .canonicalLabel(canonical)
                        .timestamp(LocalDateTime.now().toString())
                        .build())
                .build();
        topicVectorStore.putVector(request);
        log.info("[topic-aggregate] 建锚 canonical={}, typeId={}", canonical, created.getId());
        return canonical;
    }

    /** 向量 key（canonical 名唯一 → key 唯一，put 覆盖幂等）。 */
    private String anchorKey(String canonical) {
        return "topic_" + canonical;
    }
}
