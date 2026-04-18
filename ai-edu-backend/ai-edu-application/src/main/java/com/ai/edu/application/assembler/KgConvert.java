package com.ai.edu.application.assembler;

import com.ai.edu.application.dto.kg.*;
import com.ai.edu.domain.edukg.model.entity.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识图谱 DTO 转换器
 */
public final class KgConvert {

    private KgConvert() {}

    // ==================== Textbook ====================

    public static KgTextbookDTO toTextbookDTO(KgTextbook textbook) {
        if (textbook == null) return null;
        return KgTextbookDTO.builder()
                .uri(textbook.getUri())
                .label(textbook.getLabel())
                .grade(textbook.getGrade())
                .stage(textbook.getStage())
                .edition(textbook.getEdition())
                .orderIndex(textbook.getOrderIndex())
                .subject(textbook.getSubject())
                .status(textbook.getStatus())
                .build();
    }

    public static List<KgTextbookDTO> toTextbookDTOs(List<KgTextbook> textbooks) {
        if (textbooks == null) return List.of();
        return textbooks.stream().map(KgConvert::toTextbookDTO).toList();
    }

    // ==================== Chapter ====================

    public static ChapterTreeNode toChapterTreeNode(KgChapter chapter, Integer orderIndex) {
        if (chapter == null) return null;
        return ChapterTreeNode.builder()
                .uri(chapter.getUri())
                .label(chapter.getLabel())
                .topic(chapter.getTopic())
                .orderIndex(orderIndex)
                .build();
    }

    // ==================== Section ====================

    public static ChapterTreeNode.SectionNode toSectionNode(KgSection section, Integer orderIndex, int kpCount) {
        if (section == null) return null;
        return ChapterTreeNode.SectionNode.builder()
                .uri(section.getUri())
                .label(section.getLabel())
                .orderIndex(orderIndex)
                .knowledgePointCount(kpCount)
                .build();
    }

    // ==================== Knowledge Point ====================

    public static KgKnowledgePointDTO toKpDTO(KgKnowledgePoint kp) {
        if (kp == null) return null;
        return KgKnowledgePointDTO.builder()
                .uri(kp.getUri())
                .label(kp.getLabel())
                .difficulty(kp.getDifficulty())
                .importance(kp.getImportance())
                .cognitiveLevel(kp.getCognitiveLevel())
                .status(kp.getStatus())
                .build();
    }

    public static List<KgKnowledgePointDTO> toKpDTOs(List<KgKnowledgePoint> kps) {
        if (kps == null) return List.of();
        return kps.stream().map(KgConvert::toKpDTO).toList();
    }

    /**
     * 构建知识点详情 DTO（含 2 层父级）
     */
    public static KgKnowledgePointDetailDTO toKpDetailDTO(KgKnowledgePoint kp,
                                                           KgSection section,
                                                           KgChapter chapter) {
        if (kp == null) return null;
        return KgKnowledgePointDetailDTO.builder()
                .uri(kp.getUri())
                .label(kp.getLabel())
                .difficulty(kp.getDifficulty())
                .importance(kp.getImportance())
                .cognitiveLevel(kp.getCognitiveLevel())
                .sectionUri(section != null ? section.getUri() : null)
                .sectionLabel(section != null ? section.getLabel() : null)
                .chapterUri(chapter != null ? chapter.getUri() : null)
                .chapterLabel(chapter != null ? chapter.getLabel() : null)
                .build();
    }

    // ==================== Sync ====================

    public static SyncResult toSyncResult(Long syncId, String status, int inserted, int updated,
                                           int statusChanged, String reconcStatus, long duration) {
        return SyncResult.builder()
                .syncId(syncId)
                .status(status)
                .insertedCount(inserted)
                .updatedCount(updated)
                .statusChangedCount(statusChanged)
                .reconciliationStatus(reconcStatus)
                .duration(duration)
                .build();
    }

    public static SyncResult toSyncResult(KgSyncRecord record) {
        if (record == null) return null;
        long duration = 0;
        if (record.getStartedAt() != null && record.getFinishedAt() != null) {
            duration = java.time.Duration.between(record.getStartedAt(), record.getFinishedAt()).toMillis();
        }
        return SyncResult.builder()
                .syncId(record.getId())
                .status(record.getStatus())
                .insertedCount(record.getInsertedCount())
                .updatedCount(record.getUpdatedCount())
                .statusChangedCount(record.getStatusChangedCount())
                .reconciliationStatus(record.getReconciliationStatus())
                .duration(duration)
                .build();
    }

    public static SyncStatusDTO toSyncStatusDTO(KgSyncRecord record) {
        if (record == null) {
            return SyncStatusDTO.builder()
                    .status("never_synced")
                    .build();
        }
        return SyncStatusDTO.builder()
                .status("running".equals(record.getStatus()) ? "running" : "idle")
                .lastSyncAt(record.getFinishedAt() != null ? record.getFinishedAt().toString() : null)
                .lastSyncStatus(record.getStatus())
                .lastInsertedCount(record.getInsertedCount())
                .lastUpdatedCount(record.getUpdatedCount())
                .lastReconciliationStatus(record.getReconciliationStatus())
                .build();
    }

    public static SyncRecordDTO toSyncRecordDTO(KgSyncRecord record) {
        if (record == null) return null;
        return SyncRecordDTO.builder()
                .id(record.getId())
                .syncType(record.getSyncType())
                .scope(record.getScope())
                .status(record.getStatus())
                .insertedCount(record.getInsertedCount())
                .updatedCount(record.getUpdatedCount())
                .statusChangedCount(record.getStatusChangedCount())
                .reconciliationStatus(record.getReconciliationStatus())
                .errorMessage(record.getErrorMessage())
                .startedAt(record.getStartedAt() != null ? record.getStartedAt().toString() : null)
                .finishedAt(record.getFinishedAt() != null ? record.getFinishedAt().toString() : null)
                .build();
    }

    public static List<SyncRecordDTO> toSyncRecordDTOs(List<KgSyncRecord> records) {
        if (records == null) return List.of();
        return records.stream().map(KgConvert::toSyncRecordDTO).toList();
    }

    // ==================== Health ====================

    public static HealthDTO toHealthDTO(boolean available, long responseTimeMs, String message) {
        return HealthDTO.builder()
                .available(available)
                .responseTimeMs(responseTimeMs)
                .message(message)
                .build();
    }

    // ==================== Grade System ====================

    /**
     * 构建年级知识体系 DTO
     */
    public static KgGradeSystemDTO toGradeSystemDTO(String grade, String groupBy,
                                                     List<KgGradeSystemDTO.GroupDTO> groups,
                                                     int totalKp) {
        return KgGradeSystemDTO.builder()
                .grade(grade)
                .groupBy(groupBy)
                .groups(groups)
                .totalKnowledgePoints(totalKp)
                .build();
    }

    /**
     * 构建年级统计 DTO
     */
    public static StatsDTO toStatsDTO(String grade, int totalKp, int totalTb, int totalCh,
                                       int totalSec, Map<String, Integer> difficultyDist) {
        return StatsDTO.builder()
                .grade(grade)
                .totalKnowledgePoints(totalKp)
                .totalTextbooks(totalTb)
                .totalChapters(totalCh)
                .totalSections(totalSec)
                .difficultyDistribution(difficultyDist)
                .build();
    }

    /**
     * 构建批量关联 DTO
     */
    public static BatchRelationsDTO toBatchRelationsDTO(Map<String, List<String>> uriToRelated) {
        if (uriToRelated == null) return new BatchRelationsDTO();
        List<BatchRelationsDTO.RelationEntry> entries = uriToRelated.entrySet().stream()
                .map(e -> BatchRelationsDTO.RelationEntry.builder()
                        .uri(e.getKey())
                        .relatedUris(e.getValue())
                        .build())
                .toList();
        return BatchRelationsDTO.builder()
                .relations(entries)
                .build();
    }
}
