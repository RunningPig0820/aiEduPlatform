package com.ai.edu.application.service;

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
        List<KgTextbook> textbooks = kgTextbookRepository.findAllActive().stream()
                .filter(tb -> grade.equals(tb.getGrade()))
                .toList();

        if (textbooks.isEmpty()) {
            return KgConvert.toGradeSystemDTO(grade, groupBy != null ? groupBy : "subject", List.of(), 0);
        }

        String effectiveGroupBy = groupBy != null && !groupBy.isBlank() ? groupBy : "subject";

        Map<String, List<KgTextbook>> grouped = textbooks.stream()
                .collect(Collectors.groupingBy(tb ->
                        "phase".equals(effectiveGroupBy) ? tb.getPhase() : tb.getSubject()
                ));

        List<KgChapterSection> allChSec = kgChapterSectionRepository.findAllActive();
        List<KgSectionKP> allSecKp = kgSectionKPRepository.findAllActive();

        List<KgGradeSystemDTO.GroupDTO> groups = new ArrayList<>();
        int totalKp = 0;

        for (Map.Entry<String, List<KgTextbook>> entry : grouped.entrySet()) {
            String groupKey = entry.getKey();
            List<KgTextbook> groupTextbooks = entry.getValue();

            Set<String> allChapterUris = new HashSet<>();
            for (KgTextbook tb : groupTextbooks) {
                List<KgTextbookChapter> tbChRels = kgTextbookChapterRepository.findByTextbookUri(tb.getUri());
                for (KgTextbookChapter rel : tbChRels) {
                    allChapterUris.add(rel.getChapterUri());
                }
            }

            if (allChapterUris.isEmpty()) continue;

            Map<String, KgChapter> chapterMap = kgChapterRepository.findByUris(new ArrayList<>(allChapterUris)).stream()
                    .collect(Collectors.toMap(KgChapter::getUri, ch -> ch));

            List<KgGradeSystemDTO.GroupDTO.ChapterNode> chapterNodes = new ArrayList<>();

            for (String chapterUri : allChapterUris) {
                KgChapter chapter = chapterMap.get(chapterUri);
                if (chapter == null) continue;

                KgGradeSystemDTO.GroupDTO.ChapterNode chapterNode = KgGradeSystemDTO.GroupDTO.ChapterNode.builder()
                        .uri(chapter.getUri())
                        .label(chapter.getLabel())
                        .topic(chapter.getTopic())
                        .build();

                List<KgChapterSection> chSecRels = allChSec.stream()
                        .filter(r -> chapterUri.equals(r.getChapterUri()))
                        .toList();

                List<KgGradeSystemDTO.GroupDTO.SectionNode> sectionNodes = new ArrayList<>();
                Set<String> sectionUris = chSecRels.stream()
                        .map(KgChapterSection::getSectionUri)
                        .collect(Collectors.toSet());

                Map<String, KgSection> sectionMap = kgSectionRepository.findByUris(new ArrayList<>(sectionUris)).stream()
                        .collect(Collectors.toMap(KgSection::getUri, sec -> sec));

                for (KgChapterSection secRel : chSecRels) {
                    KgSection section = sectionMap.get(secRel.getSectionUri());
                    if (section == null) continue;

                    List<KgSectionKP> secKpRels = allSecKp.stream()
                            .filter(r -> secRel.getSectionUri().equals(r.getSectionUri()))
                            .toList();

                    List<KgKnowledgePointDTO> kpDTOs = new ArrayList<>();
                    for (KgSectionKP kpRel : secKpRels) {
                        KgKnowledgePoint kp = kgKnowledgePointRepository.findByUri(kpRel.getKpUri()).orElse(null);
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

            String groupLabel = "phase".equals(effectiveGroupBy) ? phaseToLabel(groupKey) : subjectToLabel(groupKey);

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
        List<KgTextbook> textbooks = kgTextbookRepository.findAllActive().stream()
                .filter(tb -> grade.equals(tb.getGrade()))
                .toList();

        if (textbooks.isEmpty()) {
            return KgConvert.toStatsDTO(grade, 0, 0, 0, 0, Map.of());
        }

        Set<String> textbookUris = textbooks.stream().map(KgTextbook::getUri).collect(Collectors.toSet());

        List<KgTextbookChapter> tbChRels = kgTextbookChapterRepository.findAllActive().stream()
                .filter(r -> textbookUris.contains(r.getTextbookUri()))
                .toList();
        Set<String> chapterUris = tbChRels.stream().map(KgTextbookChapter::getChapterUri).collect(Collectors.toSet());

        List<KgChapterSection> chSecRels = kgChapterSectionRepository.findAllActive().stream()
                .filter(r -> chapterUris.contains(r.getChapterUri()))
                .toList();
        Set<String> sectionUris = chSecRels.stream().map(KgChapterSection::getSectionUri).collect(Collectors.toSet());

        List<KgSectionKP> secKpRels = kgSectionKPRepository.findAllActive().stream()
                .filter(r -> sectionUris.contains(r.getSectionUri()))
                .toList();
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

    private String phaseToLabel(String phase) {
        return switch (phase) {
            case "primary" -> "小学";
            case "middle" -> "初中";
            case "high" -> "高中";
            default -> phase;
        };
    }

    private String subjectToLabel(String subject) {
        return switch (subject) {
            case "math" -> "数学";
            default -> subject;
        };
    }
}
