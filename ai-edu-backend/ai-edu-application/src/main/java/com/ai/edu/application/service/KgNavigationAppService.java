package com.ai.edu.application.service;

import com.ai.edu.application.assembler.KgConvert;
import com.ai.edu.application.dto.kg.ChapterTreeNode;
import com.ai.edu.application.dto.kg.KgKnowledgePointDetailDTO;
import com.ai.edu.application.dto.kg.KgTextbookDTO;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.edukg.model.entity.*;
import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
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
     * 获取教材列表
     */
    public List<KgTextbookDTO> getTextbooks(String subject, String phase) {
        List<KgTextbook> textbooks;
        if (subject != null && phase != null) {
            textbooks = kgTextbookRepository.findBySubjectAndPhase(subject, phase);
        } else if (subject != null) {
            textbooks = kgTextbookRepository.findBySubject(subject);
        } else {
            textbooks = kgTextbookRepository.findAllActive();
        }
        return KgConvert.toTextbookDTOs(textbooks);
    }

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
                .collect(Collectors.toMap(KgChapter::getUri, ch -> ch));

        // 查询所有章节-小节关联（通过章节 URI 过滤）
        List<KgChapterSection> allChSec = kgChapterSectionRepository.findAllActive();
        Map<String, List<KgChapterSection>> chSecGrouped = allChSec.stream()
                .filter(r -> chapterUris.contains(r.getChapterUri()))
                .collect(Collectors.groupingBy(KgChapterSection::getChapterUri));

        Set<String> sectionUris = chSecGrouped.values().stream()
                .flatMap(List::stream)
                .map(KgChapterSection::getSectionUri)
                .collect(Collectors.toSet());
        List<KgSection> sections = kgSectionRepository.findByUris(new ArrayList<>(sectionUris));
        Map<String, KgSection> sectionMap = sections.stream()
                .collect(Collectors.toMap(KgSection::getUri, sec -> sec));

        // 统计每个小节的知识点数
        List<KgSectionKP> allSecKp = kgSectionKPRepository.findAllActive();
        Map<String, Long> sectionKpCount = allSecKp.stream()
                .filter(r -> sectionUris.contains(r.getSectionUri()))
                .collect(Collectors.groupingBy(KgSectionKP::getSectionUri, Collectors.counting()));

        // 构建树
        List<ChapterTreeNode> treeNodes = new ArrayList<>();
        for (KgTextbookChapter rel : tbChRelations) {
            KgChapter chapter = chapterMap.get(rel.getChapterUri());
            if (chapter == null) continue;

            ChapterTreeNode chapterNode = KgConvert.toChapterTreeNode(chapter, rel.getOrderIndex());

            List<KgChapterSection> sectionRels = chSecGrouped.getOrDefault(rel.getChapterUri(), List.of());
            List<ChapterTreeNode.SectionNode> sectionNodes = new ArrayList<>();
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
                .collect(Collectors.toMap(KgKnowledgePoint::getUri, kp -> kp));

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
}
