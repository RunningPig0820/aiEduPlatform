package com.ai.edu.application.service.kg;

import com.ai.edu.application.assembler.KgConvert;
import com.ai.edu.application.dto.kg.*;
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
 * 知识体系应用服务
 * 职责：年级知识体系查询、知识点统计
 */
@Slf4j
@Service
public class KgKnowledgeSystemAppService {

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
     * 获取某年级完整知识体系
     */
    public KgGradeSystemDTO getGradeSystem(String grade, String groupBy) {
        List<KgTextbook> textbooks = kgTextbookRepository.findAllActiveByGrade(grade);

        if (textbooks.isEmpty()) {
            return KgConvert.toGradeSystemDTO(grade, groupBy != null ? groupBy : "subject", List.of(), 0);
        }

        String effectiveGroupBy = groupBy != null && !groupBy.isBlank() ? groupBy : "subject";

        Map<String, List<KgTextbook>> grouped = textbooks.stream()
                .collect(Collectors.groupingBy(tb ->
                        "stage".equals(effectiveGroupBy) ? tb.getStage() : tb.getSubject()
                ));

        // 收集所有教材 URI，用于批量查询章节关联
        Set<String> allTextbookUris = textbooks.stream()
                .map(KgTextbook::getUri)
                .collect(Collectors.toSet());
        List<KgTextbookChapter> allTbCh = kgTextbookChapterRepository.findByTextbookUris(new ArrayList<>(allTextbookUris));

        // 收集所有章节 URI，用于批量查询章节-小节关联
        Set<String> allChapterUris = allTbCh.stream()
                .map(KgTextbookChapter::getChapterUri)
                .collect(Collectors.toSet());
        List<KgChapterSection> allChSec = kgChapterSectionRepository.findByChapterUris(new ArrayList<>(allChapterUris));

        // 收集所有小节 URI，用于批量查询小节-知识点关联
        Set<String> allSectionUris = allChSec.stream()
                .map(KgChapterSection::getSectionUri)
                .collect(Collectors.toSet());
        List<KgSectionKP> allSecKp = kgSectionKPRepository.findBySectionUris(new ArrayList<>(allSectionUris));

        // 预构建 Map，避免循环内重复查询
        Map<String, List<KgTextbookChapter>> tbChByTextbook = allTbCh.stream()
                .collect(Collectors.groupingBy(KgTextbookChapter::getTextbookUri));
        Map<String, List<KgChapterSection>> chSecByChapter = allChSec.stream()
                .collect(Collectors.groupingBy(KgChapterSection::getChapterUri));
        Map<String, List<KgSectionKP>> secKpBySection = allSecKp.stream()
                .collect(Collectors.groupingBy(KgSectionKP::getSectionUri));
        Map<String, KgChapter> chapterMap = kgChapterRepository.findByUris(new ArrayList<>(allChapterUris)).stream()
                .collect(Collectors.toMap(KgChapter::getUri, ch -> ch, (existing, replacement) -> existing));
        Map<String, KgSection> sectionMap = kgSectionRepository.findByUris(new ArrayList<>(allSectionUris)).stream()
                .collect(Collectors.toMap(KgSection::getUri, sec -> sec, (existing, replacement) -> existing));
        Set<String> allKpUris = allSecKp.stream()
                .map(KgSectionKP::getKpUri)
                .collect(Collectors.toSet());
        Map<String, KgKnowledgePoint> kpMap = kgKnowledgePointRepository.findByUris(new ArrayList<>(allKpUris)).stream()
                .collect(Collectors.toMap(KgKnowledgePoint::getUri, kp -> kp, (existing, replacement) -> existing));

        List<KgGradeSystemDTO.GroupDTO> groups = new ArrayList<>();
        int totalKp = 0;

        for (Map.Entry<String, List<KgTextbook>> entry : grouped.entrySet()) {
            String groupKey = entry.getKey();
            List<KgTextbook> groupTextbooks = entry.getValue();

            Set<String> groupChapterUris = new HashSet<>();
            for (KgTextbook tb : groupTextbooks) {
                List<KgTextbookChapter> tbChRels = tbChByTextbook.getOrDefault(tb.getUri(), List.of());
                for (KgTextbookChapter rel : tbChRels) {
                    groupChapterUris.add(rel.getChapterUri());
                }
            }

            if (groupChapterUris.isEmpty()) continue;

            List<KgGradeSystemDTO.GroupDTO.ChapterNode> chapterNodes = new ArrayList<>();

            for (String chapterUri : groupChapterUris) {
                KgChapter chapter = chapterMap.get(chapterUri);
                if (chapter == null) continue;

                KgGradeSystemDTO.GroupDTO.ChapterNode chapterNode = KgGradeSystemDTO.GroupDTO.ChapterNode.builder()
                        .uri(chapter.getUri())
                        .label(chapter.getLabel())
                        .topic(chapter.getTopic())
                        .build();

                List<KgChapterSection> chSecRels = chSecByChapter.getOrDefault(chapterUri, List.of());

                List<KgGradeSystemDTO.GroupDTO.SectionNode> sectionNodes = new ArrayList<>();

                for (KgChapterSection secRel : chSecRels) {
                    KgSection section = sectionMap.get(secRel.getSectionUri());
                    if (section == null) continue;

                    List<KgSectionKP> secKpRels = secKpBySection.getOrDefault(secRel.getSectionUri(), List.of());

                    List<KgKnowledgePointDTO> kpDTOs = new ArrayList<>();
                    for (KgSectionKP kpRel : secKpRels) {
                        KgKnowledgePoint kp = kpMap.get(kpRel.getKpUri());
                        if (kp != null) {
                            kpDTOs.add(KgConvert.toKpDTO(kp));
                        }
                    }

                    sectionNodes.add(KgGradeSystemDTO.GroupDTO.SectionNode.builder()
                            .uri(section.getUri())
                            .label(section.getLabel())
                            .knowledgePoints(kpDTOs)
                            .build());
                }

                chapterNode.setSections(sectionNodes);
                chapterNodes.add(chapterNode);
            }

            int groupKpCount = chapterNodes.stream()
                    .flatMap(cn -> cn.getSections() != null ? cn.getSections().stream() : java.util.stream.Stream.empty())
                    .mapToInt(sn -> sn.getKnowledgePoints() != null ? sn.getKnowledgePoints().size() : 0)
                    .sum();
            totalKp += groupKpCount;

            String groupLabel = "stage".equals(effectiveGroupBy) ? stageToLabel(groupKey) : subjectToLabel(groupKey);

            groups.add(KgGradeSystemDTO.GroupDTO.builder()
                    .key(groupKey)
                    .label(groupLabel)
                    .chapters(chapterNodes)
                    .knowledgePointCount(groupKpCount)
                    .build());
        }

        return KgConvert.toGradeSystemDTO(grade, effectiveGroupBy, groups, totalKp);
    }

    /**
     * 获取年级知识点统计
     */
    public StatsDTO getGradeStats(String grade) {
        List<KgTextbook> textbooks = kgTextbookRepository.findAllActiveByGrade(grade);

        if (textbooks.isEmpty()) {
            return KgConvert.toStatsDTO(grade, 0, 0, 0, 0, Map.of());
        }

        Set<String> textbookUris = textbooks.stream().map(KgTextbook::getUri).collect(Collectors.toSet());

        List<KgTextbookChapter> tbChRels = kgTextbookChapterRepository.findByTextbookUris(new ArrayList<>(textbookUris));
        Set<String> chapterUris = tbChRels.stream().map(KgTextbookChapter::getChapterUri).collect(Collectors.toSet());

        List<KgChapterSection> chSecRels = kgChapterSectionRepository.findByChapterUris(new ArrayList<>(chapterUris));
        Set<String> sectionUris = chSecRels.stream().map(KgChapterSection::getSectionUri).collect(Collectors.toSet());

        List<KgSectionKP> secKpRels = kgSectionKPRepository.findBySectionUris(new ArrayList<>(sectionUris));
        Set<String> kpUris = secKpRels.stream().map(KgSectionKP::getKpUri).collect(Collectors.toSet());

        List<KgKnowledgePoint> kps = kgKnowledgePointRepository.findByUris(new ArrayList<>(kpUris));
        Map<String, Integer> difficultyDist = kps.stream()
                .collect(Collectors.groupingBy(
                        kp -> kp.getDifficulty() != null && !kp.getDifficulty().isBlank() ? kp.getDifficulty() : "unknown",
                        Collectors.summingInt(kp -> 1)
                ));

        return KgConvert.toStatsDTO(
                grade,
                kpUris.size(),
                textbooks.size(),
                chapterUris.size(),
                sectionUris.size(),
                difficultyDist
        );
    }

    private String stageToLabel(String stage) {
        return switch (stage) {
            case "primary" -> "小学";
            case "middle" -> "初中";
            case "high" -> "高中";
            default -> stage;
        };
    }

    private String subjectToLabel(String subject) {
        return switch (subject) {
            case "math" -> "数学";
            default -> subject;
        };
    }
}
