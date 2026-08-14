package com.ai.edu.application.service.batch;

import com.ai.edu.domain.learning.model.entity.DerivedKpObs;
import com.ai.edu.domain.learning.model.valueobject.DerivedKpStatus;
import com.ai.edu.domain.learning.model.valueobject.KpResolution;
import com.ai.edu.domain.learning.repository.DerivedKpObsRepository;
import com.ai.edu.domain.learning.service.KpDisambiguationPort;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 【离线处理 · 大数据归宿】知识点派生层维护闭环服务（周期重判 + 先验漂移）。
 *
 * <p>周期任务：① 第二信号共现转正（WEAK→RESOLVED）→ ② 冲突 LLM 重判（CONFLICTED→READJUDICATED/HUMAN_REVIEW）
 * → ③ 统计回流（重新聚合题型库，先验漂移）。
 *
 * <p><b>逻辑归宿：大数据平台（离线批处理）。</b>当前后端 @Scheduled 过渡实现，未来迁大数据平台（见 package-info）。
 *
 * <p><b>尚未实现（后续）</b>：① 冲突检测的完整信号（decide 诊断冲突 / 掌握度矛盾 / 做题结果矛盾打 CONFLICTED）
 * 需接入 decide 诊断与掌握度数据；② 掌握度错解析 MIGRATED 打标需掌握度表 schema 变更（本任务只做 HUMAN_REVIEW）。
 */
@Slf4j
@Service
public class KpMaintenanceService {

    /** 共现转正的学生数阈值（第二独立信号）。 */
    private static final int COOCCURRENCE_STUDENTS = 2;

    @Resource
    private DerivedKpObsRepository derivedKpObsRepository;
    @Resource
    private KpDisambiguationPort kpDisambiguationPort;
    @Resource
    private KpQuestionTypeAggregationService aggregationService;

    @Value("${ai-edu.kp.confidence-threshold:60}")
    private int confidenceThreshold;

    /**
     * 维护闭环（周期）：第二信号转正 → 冲突重判 → 统计回流。
     * 凌晨 3:37 触发（避开聚合 3:17）。
     */
    @Scheduled(cron = "0 37 3 * * ?")
    public void maintain() {
        promoteWeakByCooccurrence();
        rejudgeConflicted();
        aggregationService.aggregate();
    }

    /** 第二信号共现转正：冷启动 WEAK 观测，同题型同知识点已有 ≥2 名不同学生 → 转 RESOLVED。 */
    private void promoteWeakByCooccurrence() {
        List<DerivedKpObs> weakObs = derivedKpObsRepository.findByStatus(DerivedKpStatus.WEAK);
        for (DerivedKpObs obs : weakObs) {
            if (obs.getKpUri() == null || obs.getKpUri().isBlank()) {
                continue;
            }
            int students = derivedKpObsRepository.countDistinctStudentsByTopicAndKp(obs.getTopicLabel(), obs.getKpUri());
            if (students >= COOCCURRENCE_STUDENTS) {
                derivedKpObsRepository.updateStatus(obs.getId(), DerivedKpStatus.RESOLVED);
                log.info("冷启动弱确定转正（共现）: topic={}, kp={}, students={}",
                        obs.getTopicLabel(), obs.getKpUri(), students);
            }
        }
    }

    /** 冲突重判：CONFLICTED 观测 → LLM 重判 → 高置信 READJUDICATED / 仍歧义 HUMAN_REVIEW。 */
    private void rejudgeConflicted() {
        List<DerivedKpObs> conflicted = derivedKpObsRepository.findByStatus(DerivedKpStatus.CONFLICTED);
        for (DerivedKpObs obs : conflicted) {
            try {
                rejudge(obs);
            } catch (Exception e) {
                log.warn("重判观测失败: obsId={}", obs.getId(), e);
            }
        }
    }

    private void rejudge(DerivedKpObs obs) {
        KpResolution result = kpDisambiguationPort.disambiguate(obs.getTopicLabel(), obs.getStudentGrade());
        if (result != null && result.getConfidence() >= confidenceThreshold) {
            // 保守原则：只有高置信重判才自动改
            derivedKpObsRepository.updateStatus(obs.getId(), DerivedKpStatus.READJUDICATED);
            log.info("冲突观测重判（高置信）: topic={}, kp={} -> {}", obs.getTopicLabel(), obs.getKpUri(), result.getUri());
        } else {
            // 仍歧义 → 转人工（不自动改）
            derivedKpObsRepository.updateStatus(obs.getId(), DerivedKpStatus.HUMAN_REVIEW);
            log.info("冲突观测重判仍歧义（转人工）: topic={}", obs.getTopicLabel());
        }
    }
}
