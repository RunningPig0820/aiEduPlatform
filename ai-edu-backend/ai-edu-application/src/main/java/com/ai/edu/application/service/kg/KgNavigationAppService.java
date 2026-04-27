package com.ai.edu.application.service.kg;

import com.ai.edu.application.assembler.KgConvert;
import com.ai.edu.application.dto.kg.*;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.edukg.model.entity.*;
import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
import com.ai.edu.domain.edukg.model.valueobject.KgStageEnum;
import com.ai.edu.domain.edukg.model.valueobject.KgSubjectEnum;
import com.ai.edu.domain.edukg.model.valueobject.KgTextbookEnum;
import com.ai.edu.domain.edukg.repository.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识图谱导航查询应用服务
 * 职责：编排导航查询，组装树形结构和详情
 */
@Slf4j
@Service
public class KgNavigationAppService {

    @Resource
    private KgTextbookRepository kgTextbookRepository;
    @Resource
    private KgChapterRepository kgChapterRepository;
    @Resource
    private KgSectionRepository kgSectionRepository;
    @Resource
    private KgKnowledgePointRepository kgKnowledgePointRepository;
    @Resource
    private KgTextbookChapterRepository kgTextbookChapterRepository;
    @Resource
    private KgChapterSectionRepository kgChapterSectionRepository;
    @Resource
    private KgSectionKPRepository kgSectionKPRepository;


    /**
     * 获取教材详情
     */
    public KgTextbookDTO getTextbookDetail(String uri) {
        KgTextbook textbook = kgTextbookRepository.findByUri(uri)
                .orElseThrow(() -> new BusinessException(ErrorCode.KG_TEXTBOOK_NOT_FOUND, "教材不存在: " + uri));
        return KgConvert.toTextbookDTO(textbook);
    }

    /**
     * 获取教材章节树
     */
    public List<ChapterTreeNode> getChaptersByTextbook(String textbookUri) {
        kgTextbookRepository.findByUri(textbookUri)
                .orElseThrow(() -> new BusinessException(ErrorCode.KG_TEXTBOOK_NOT_FOUND, "教材不存在: " + textbookUri));

        List<KgTextbookChapter> tbChRelations = kgTextbookChapterRepository.findByTextbookUri(textbookUri);
        if (tbChRelations.isEmpty()) {
            return List.of();
        }

        Set<String> chapterUris = tbChRelations.stream()
                .map(KgTextbookChapter::getChapterUri)
                .collect(Collectors.toSet());
        List<KgChapter> chapters = kgChapterRepository.findByUris(new ArrayList<>(chapterUris));
        Map<String, KgChapter> chapterMap = chapters.stream()
                .collect(Collectors.toMap(KgChapter::getUri, ch -> ch, (existing, replacement) -> existing));

        // 按章节 URI 列表批量查询章节-小节关联（避免全量查询）
        List<KgChapterSection> chSecRelations = kgChapterSectionRepository.findByChapterUris(new ArrayList<>(chapterUris));
        Map<String, List<KgChapterSection>> chSecGrouped = chSecRelations.stream()
                .collect(Collectors.groupingBy(KgChapterSection::getChapterUri));

        Set<String> sectionUris = chSecRelations.stream()
                .map(KgChapterSection::getSectionUri)
                .collect(Collectors.toSet());
        List<KgSection> sections = kgSectionRepository.findByUris(new ArrayList<>(sectionUris));
        Map<String, KgSection> sectionMap = sections.stream()
                .collect(Collectors.toMap(KgSection::getUri, sec -> sec, (existing, replacement) -> existing));

        // 按小节 URI 列表批量查询小节-知识点关联（避免全量查询）
        List<KgSectionKP> secKpRelations = kgSectionKPRepository.findBySectionUris(new ArrayList<>(sectionUris));
        Map<String, Long> sectionKpCount = secKpRelations.stream()
                .collect(Collectors.groupingBy(KgSectionKP::getSectionUri, Collectors.counting()));

        // 构建树
        List<ChapterTreeNode> treeNodes = new ArrayList<>();
        for (KgTextbookChapter rel : tbChRelations) {
            KgChapter chapter = chapterMap.get(rel.getChapterUri());
            if (chapter == null) continue;

            ChapterTreeNode chapterNode = KgConvert.toChapterTreeNode(chapter, rel.getOrderIndex());

            List<KgChapterSection> sectionRels = chSecGrouped.getOrDefault(rel.getChapterUri(), List.of());
            List<SectionNode> sectionNodes = new ArrayList<>();
            for (KgChapterSection secRel : sectionRels) {
                KgSection section = sectionMap.get(secRel.getSectionUri());
                if (section == null) continue;
                int kpCount = sectionKpCount.getOrDefault(secRel.getSectionUri(), 0L).intValue();
                sectionNodes.add(KgConvert.toSectionNode(section, secRel.getOrderIndex(), kpCount));
            }

            // 空章节过滤
            if (!sectionNodes.isEmpty()) {
                chapterNode.setSections(sectionNodes);
                treeNodes.add(chapterNode);
            }
        }

        return treeNodes;
    }

    /**
     * 获取章节下的小节列表
     */
    public List<SectionNode> getSectionsByChapter(String chapterUri) {
        kgChapterRepository.findByUri(chapterUri)
                .orElseThrow(() -> new BusinessException(ErrorCode.KG_CHAPTER_NOT_FOUND, "章节不存在: " + chapterUri));

        List<KgChapterSection> chSecRelations = kgChapterSectionRepository.findByChapterUri(chapterUri);
        if (chSecRelations.isEmpty()) {
            return List.of();
        }

        Set<String> sectionUris = chSecRelations.stream()
                .map(KgChapterSection::getSectionUri)
                .collect(Collectors.toSet());
        List<KgSection> sections = kgSectionRepository.findByUris(new ArrayList<>(sectionUris));
        Map<String, KgSection> sectionMap = sections.stream()
                .collect(Collectors.toMap(KgSection::getUri, sec -> sec, (existing, replacement) -> existing));

        // 按小节 URI 列表批量查询小节-知识点关联
        List<KgSectionKP> secKpRelations = kgSectionKPRepository.findBySectionUris(new ArrayList<>(sectionUris));
        Map<String, Long> sectionKpCount = secKpRelations.stream()
                .collect(Collectors.groupingBy(KgSectionKP::getSectionUri, Collectors.counting()));

        List<SectionNode> sectionNodes = new ArrayList<>();
        for (KgChapterSection rel : chSecRelations) {
            KgSection section = sectionMap.get(rel.getSectionUri());
            if (section == null) continue;
            int kpCount = sectionKpCount.getOrDefault(rel.getSectionUri(), 0L).intValue();
            sectionNodes.add(KgConvert.toSectionNode(section, rel.getOrderIndex(), kpCount));
        }

        return sectionNodes;
    }

    /**
     * 获取小节知识点列表
     */
    public List<KgKnowledgePointDetailDTO> getKnowledgePointsBySection(String sectionUri) {
        List<KgSectionKP> secKpRelations = kgSectionKPRepository.findBySectionUri(sectionUri);
        if (secKpRelations.isEmpty()) {
            return List.of();
        }

        Set<String> kpUris = secKpRelations.stream()
                .map(KgSectionKP::getKpUri)
                .collect(Collectors.toSet());
        List<KgKnowledgePoint> kps = kgKnowledgePointRepository.findByUris(new ArrayList<>(kpUris));
        Map<String, KgKnowledgePoint> kpMap = kps.stream()
                .collect(Collectors.toMap(KgKnowledgePoint::getUri, kp -> kp, (existing, replacement) -> existing));

        KgSection section = kgSectionRepository.findByUri(sectionUri).orElse(null);

        KgChapter chapter = null;
        List<KgChapterSection> chSecRels = kgChapterSectionRepository.findBySectionUri(sectionUri);
        if (!chSecRels.isEmpty()) {
            chapter = kgChapterRepository.findByUri(chSecRels.get(0).getChapterUri()).orElse(null);
        }

        List<KgKnowledgePointDetailDTO> results = new ArrayList<>();
        for (KgSectionKP rel : secKpRelations) {
            KgKnowledgePoint kp = kpMap.get(rel.getKpUri());
            if (kp == null) continue;
            results.add(KgConvert.toKpDetailDTO(kp, section, chapter));
        }
        return results;
    }

    /**
     * 获取知识点详情（含 2 层父级）
     */
    public KgKnowledgePointDetailDTO getKnowledgePointDetail(String kpUri) {
        KgKnowledgePoint kp = kgKnowledgePointRepository.findByUri(kpUri)
                .orElseThrow(() -> new BusinessException(ErrorCode.KG_KNOWLEDGE_POINT_NOT_FOUND, "知识点不存在: " + kpUri));

        List<KgSectionKP> secKpRels = kgSectionKPRepository.findByKpUri(kpUri);
        KgSection section = null;
        KgChapter chapter = null;

        if (!secKpRels.isEmpty()) {
            String sectionUri = secKpRels.get(0).getSectionUri();
            section = kgSectionRepository.findByUri(sectionUri).orElse(null);

            List<KgChapterSection> chSecRels = kgChapterSectionRepository.findBySectionUri(sectionUri);
            if (!chSecRels.isEmpty()) {
                String chapterUri = chSecRels.get(0).getChapterUri();
                chapter = kgChapterRepository.findByUri(chapterUri).orElse(null);
            }
        }

        return KgConvert.toKpDetailDTO(kp, section, chapter);
    }

    // ==================== 维度配置（下拉选择器） ====================

    /**
     * 获取学科列表（从枚举读取）
     */
    public List<KgDimensionDTO> getSubjects() {
        return Arrays.stream(KgSubjectEnum.values())
                .sorted(Comparator.comparingInt(KgSubjectEnum::getOrderIndex))
                .map(e -> KgDimensionDTO.builder()
                        .code(e.getCode())
                        .label(e.getLabel())
                        .orderIndex(e.getOrderIndex())
                        .build())
                .toList();
    }

    /**
     * 按版本/学科筛选获取年级列表（从 MySQL 聚合查询）
     */
    public List<String> getGradesByEditionSubject(String edition, String subject) {
        return kgTextbookRepository.findDistinctGradesByEditionSubject(edition, subject);
    }

    /**
     * 按版本+学科获取年级+教材URI列表（用于下拉选择器）
     */
    public List<GradeTextbookDTO> getGradeTextbooks(String edition, String subject) {
        List<KgTextbook> textbooks = kgTextbookRepository.findByEditionSubject(edition, subject);
        return textbooks.stream()
                .map(tb -> GradeTextbookDTO.builder()
                        .grade(tb.getGrade())
                        .label(tb.getLabel())
                        .textbookUri(tb.getUri())
                        .build())
                .toList();
    }

    /**
     * 获取学段列表（从枚举读取）
     */
    public List<KgDimensionDTO> getStages() {
        return Arrays.stream(KgStageEnum.values())
                .sorted(Comparator.comparingInt(KgStageEnum::getOrderIndex))
                .map(e -> KgDimensionDTO.builder()
                        .code(e.getCode())
                        .label(e.getLabel())
                        .orderIndex(e.getOrderIndex())
                        .build())
                .toList();
    }

    /**
     * 获取教材版本列表（从枚举读取）
     */
    public List<KgDimensionDTO> getTextbooks() {
        return Arrays.stream(KgTextbookEnum.values())
                .sorted(Comparator.comparingInt(KgTextbookEnum::getOrderIndex))
                .map(e -> KgDimensionDTO.builder()
                        .code(e.getCode())
                        .label(e.getDesc())
                        .orderIndex(e.getOrderIndex())
                        .build())
                .toList();
    }

    /**
     * 获取学科下的年级列表（从 MySQL 聚合查询）
     */
    public List<String> getGradesBySubject(String subject) {
        return kgTextbookRepository.findDistinctGradesBySubject(subject);
    }

    /**
     * 获取年级下的教材列表（从 MySQL 查询）
     */
    public List<KgTextbookDTO> getTextbooksByGrade(String grade) {
        List<KgTextbook> textbooks = kgTextbookRepository.findAllActiveByGrade(grade);
        return KgConvert.toTextbookDTOs(textbooks);
    }
}
