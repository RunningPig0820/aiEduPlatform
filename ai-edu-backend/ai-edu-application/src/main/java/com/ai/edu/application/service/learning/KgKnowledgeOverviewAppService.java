package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.KgKnowledgePointPageItemDTO;
import com.ai.edu.application.dto.learning.PageDTO;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.edukg.model.valueobject.KgStageEnum;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import jakarta.annotation.Resource;
import lombok.AccessLevel;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识点总览查询应用服务（学生端"知识点总览"知识地图底图）。
 *
 * <p>按学段分页列教材知识点（带章节/小节归属），数据源 kg 镜像只读。
 */
@Service
public class KgKnowledgeOverviewAppService {

    @Resource
    @Setter(AccessLevel.PACKAGE)
    private KgKnowledgePointRepository kgKnowledgePointRepository;

    /** 按学段分页列教材知识点。 */
    public PageDTO<KgKnowledgePointPageItemDTO> page(String stage, int page, int size) {
        KgStageEnum stageEnum = resolveStage(stage);
        // 库里 t_kg_textbook.stage 存中文 label，查询前 code → 中文
        String stageLabel = stageEnum.getLabel();
        long total = kgKnowledgePointRepository.countByStage(stageLabel);
        List<KgKnowledgePointPageItemDTO> items = kgKnowledgePointRepository
                .findPageByStage(stageLabel, (page - 1) * size, size).stream()
                .map(p -> KgKnowledgePointPageItemDTO.builder()
                        .kpUri(p.getKpUri())
                        .kpLabel(p.getKpLabel())
                        .stage(stageEnum.getCode())
                        .chapterLabel(p.getChapterLabel())
                        .sectionLabel(p.getSectionLabel())
                        .build())
                .toList();
        return PageDTO.<KgKnowledgePointPageItemDTO>builder()
                .items(items).total(total).page(page).size(size).build();
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
