package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.KgTreeNodeDTO;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.edukg.model.valueobject.KgStageEnum;
import com.ai.edu.domain.edukg.model.valueobject.KgTreeNode;
import com.ai.edu.domain.edukg.repository.KgOverviewTreeRepository;
import jakarta.annotation.Resource;
import lombok.AccessLevel;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识点总览应用服务（知识地图底图，点击式下钻）。
 *
 * <p>学段→课本→章节→小节→知识点逐层下钻，每次单层查询（≤2 表 JOIN，索引命中），
 * 替代原「学段→知识点」7 表 JOIN 分页（慢 SQL：GROUP BY 先于 LIMIT + COUNT DISTINCT 全扫）。
 * 数据源 kg 镜像只读。
 */
@Service
public class KgKnowledgeOverviewAppService {

    @Resource
    @Setter(AccessLevel.PACKAGE)
    private KgOverviewTreeRepository kgOverviewTreeRepository;

    /** 学段 → 年级列表（stage code → 中文查库）。 */
    public List<KgTreeNodeDTO> gradesByStage(String stage) {
        KgStageEnum stageEnum = resolveStage(stage);
        return kgOverviewTreeRepository.findGradesByStage(stageEnum.getLabel()).stream()
                .map(this::toDto).toList();
    }

    /** 学段 + 年级 → 课本列表（stage code → 中文查库）。 */
    public List<KgTreeNodeDTO> textbooksByStage(String stage, String grade) {
        KgStageEnum stageEnum = resolveStage(stage);
        return kgOverviewTreeRepository.findTextbooksByStage(stageEnum.getLabel(), grade).stream()
                .map(this::toDto).toList();
    }

    /** 课本 → 章节列表。 */
    public List<KgTreeNodeDTO> chaptersByTextbook(String textbookUri) {
        return kgOverviewTreeRepository.findChaptersByTextbookUri(textbookUri).stream()
                .map(this::toDto).toList();
    }

    /** 章节 → 小节列表。 */
    public List<KgTreeNodeDTO> sectionsByChapter(String chapterUri) {
        return kgOverviewTreeRepository.findSectionsByChapterUri(chapterUri).stream()
                .map(this::toDto).toList();
    }

    /** 小节 → 知识点列表。 */
    public List<KgTreeNodeDTO> knowledgePointsBySection(String sectionUri) {
        return kgOverviewTreeRepository.findKnowledgePointsBySectionUri(sectionUri).stream()
                .map(this::toDto).toList();
    }

    private KgTreeNodeDTO toDto(KgTreeNode n) {
        return KgTreeNodeDTO.builder()
                .uri(n.getUri()).label(n.getLabel()).orderIndex(n.getOrderIndex())
                .build();
    }

    /** 校验 stage code 并返回枚举；非法抛 INVALID_PARAMS。 */
    private KgStageEnum resolveStage(String stage) {
        KgStageEnum e = KgStageEnum.fromCode(stage);
        if (e == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "stage 非法，应为 primary/middle/high");
        }
        return e;
    }
}
