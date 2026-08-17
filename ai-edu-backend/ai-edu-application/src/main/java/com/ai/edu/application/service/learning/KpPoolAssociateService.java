package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.QuestionAnalysisDTO;
import com.ai.edu.application.dto.learning.QuestionAnalysisKpDTO;
import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.model.valueobject.KgStageEnum;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.learning.model.entity.DerivedKpObs;
import com.ai.edu.domain.learning.model.valueobject.DerivedKpSource;
import com.ai.edu.domain.learning.model.valueobject.DerivedKpStatus;
import com.ai.edu.domain.learning.repository.DerivedKpObsRepository;
import com.ai.edu.domain.learning.service.KpConstrainedAssociationPort;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 封闭域池约束选择编排（「题库和知识点」独立迭代用，analyze 本期未接线）。
 *
 * <p>D8 逻辑：题型库 miss → 取学段知识点 label 池 → 粗筛子池（池 >200 缩容）→
 * LLM 只能从池里选 top-N（恒非空）→ top-1 落 RESOLVED obs（信任模型：无确认也进数据喂聚合）。
 * 本期 analyze 不做此步（前端降级：题库 miss → PENDING，空可接受）；
 * 组件（{@link KpConstrainedAssociationPort} / findLabelsByStage / 本编排）已交付，
 * 「题库和知识点」迭代启动时在 analyze ② 处接线即可。
 */
@Slf4j
@Service
public class KpPoolAssociateService {

    /** 池约束选择上限：学段知识点池超过则粗筛缩容，LLM 上下文可控。 */
    private static final int POOL_LIMIT = 200;
    /** 池猜测的占比启发（top-N 降序），数据准确后由聚合/观测校准。 */
    private static final double[] TOP_N_RATIO = {0.6, 0.3, 0.1};

    @Resource
    private KgKnowledgePointRepository kgKnowledgePointRepository;
    @Resource
    private DerivedKpObsRepository derivedKpObsRepository;
    @Resource
    private KpConstrainedAssociationPort constrainedAssociationPort;

    /**
     * 池约束选择编排：学段池 → 粗筛子池 → LLM 从池选 top-N → top-1 落 RESOLVED obs。
     *
     * @return 池选择结果（RESOLVED + top-N knowledgePoints）；无学段/池空/选择为空返回 null（调用方走 PENDING 兜底）
     */
    public QuestionAnalysisDTO associate(String text, Long studentId, Integer grade, List<String> topics) {
        if (topics == null || topics.isEmpty()) {
            return null;
        }
        String stageLabel = stageLabelOf(grade);
        if (stageLabel == null) {
            return null;
        }
        List<String> pool = kgKnowledgePointRepository.findLabelsByStage(stageLabel);
        if (pool.isEmpty()) {
            return null;
        }
        List<String> subPool = coarseFilter(pool, text, topics);
        // LLM 只能从池里选，恒非空（失败回退池前 N）
        List<String> topN = constrainedAssociationPort.associate(text, grade, subPool);
        if (topN.isEmpty()) {
            return null;
        }
        // 信任模型：top-1 直接落 RESOLVED obs（无确认也进数据喂聚合）；学生确认=正确，vote 覆盖纠正
        persistTop1Obs(studentId, topics.get(0), grade, topN);
        return QuestionAnalysisDTO.resolved(topics.get(0), 60, kpsFrom(topN));
    }

    /** 粗筛：池超过上限时用题目 n-gram + 题型名召回相关子池；召回空回退全池截断。 */
    private List<String> coarseFilter(List<String> pool, String text, List<String> topics) {
        if (pool.size() <= POOL_LIMIT) {
            return pool;
        }
        Set<String> grams = ngrams(text);
        for (String t : topics) {
            grams.add(t);
        }
        List<String> recalled = pool.stream()
                .filter(label -> grams.stream().anyMatch(label::contains))
                .limit(POOL_LIMIT)
                .toList();
        if (!recalled.isEmpty()) {
            return recalled;
        }
        return pool.stream().limit(POOL_LIMIT).toList();
    }

    /** 题目 2~4 字连续子串（去标点/数字/空白），作粗筛召回关键词。 */
    private Set<String> ngrams(String text) {
        String cleaned = text == null ? "" : text.replaceAll("[\\p{Punct}\\d\\s]", "");
        Set<String> grams = new HashSet<>();
        int maxLen = Math.min(4, cleaned.length());
        for (int len = 2; len <= maxLen; len++) {
            for (int i = 0; i <= cleaned.length() - len; i++) {
                grams.add(cleaned.substring(i, i + len));
            }
        }
        return grams;
    }

    /** 年级 → 学段中文 label（小学/初中/高中）；不可得返回 null（无池，调用方走 PENDING）。 */
    private String stageLabelOf(Integer grade) {
        if (grade == null) {
            return null;
        }
        if (grade <= 6) {
            return KgStageEnum.PRIMARY.getLabel();
        }
        if (grade <= 9) {
            return KgStageEnum.MIDDLE.getLabel();
        }
        return KgStageEnum.HIGH.getLabel();
    }

    /** 信任模型：top-1 落 RESOLVED obs（进数据喂聚合）；studentId 为空（防御）不写。 */
    private void persistTop1Obs(Long studentId, String topicLabel, Integer grade, List<String> topN) {
        if (studentId == null || topN.isEmpty()) {
            return;
        }
        String top1Uri = kgKnowledgePointRepository.findByLabel(topN.get(0))
                .map(KgKnowledgePoint::getUri)
                .orElse(null);
        if (top1Uri == null) {
            return;
        }
        DerivedKpObs obs = DerivedKpObs.create(studentId, topicLabel, top1Uri, grade, 60,
                DerivedKpSource.LLM, DerivedKpStatus.RESOLVED);
        derivedKpObsRepository.upsert(obs);
        log.debug("analyze 池约束 top-1 落观测: studentId={}, topic={}, kp={}", studentId, topicLabel, top1Uri);
    }

    /** top-N label → knowledgePoints DTO（URI 镜像反查，ratio 降序启发）。 */
    private List<QuestionAnalysisKpDTO> kpsFrom(List<String> topN) {
        List<QuestionAnalysisKpDTO> items = new ArrayList<>();
        for (int i = 0; i < topN.size(); i++) {
            String label = topN.get(i);
            String uri = kgKnowledgePointRepository.findByLabel(label)
                    .map(KgKnowledgePoint::getUri)
                    .orElse(null);
            items.add(QuestionAnalysisKpDTO.builder()
                    .kpUri(uri)
                    .kpLabel(label)
                    .gradeRange(null)
                    .ratio(i < TOP_N_RATIO.length ? TOP_N_RATIO[i] : 0.1)
                    .build());
        }
        return items;
    }
}
