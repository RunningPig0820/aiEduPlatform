package com.ai.edu.domain.edukg.model.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 知识图谱-同步记录实体
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgSyncRecord {

    private Long id;

    private String syncType;

    private String edition;

    private String subject;

    private String stage;

    private String grade;

    /**
     * @deprecated 使用 edition/subject/stage/grade 维度字段替代
     */
    @Deprecated
    private String scope;

    private String status;

    private Integer insertedCount = 0;

    private Integer updatedCount = 0;

    private Integer statusChangedCount = 0;

    private String reconciliationStatus;

    private String reconciliationDetails;

    private String errorMessage;

    private String details;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private Long createdBy = 0L;

    private Long modifiedBy = 0L;

    private Boolean deleted = false;

    public static KgSyncRecord create(String syncType, String edition, String subject,
                                       String stage, String grade, Long createdBy) {
        KgSyncRecord record = new KgSyncRecord();
        record.syncType = syncType;
        record.edition = edition;
        record.subject = subject;
        record.stage = stage;
        record.grade = grade;
        record.status = "running";
        record.createdBy = createdBy;
        record.startedAt = LocalDateTime.now();
        return record;
    }

    /**
     * 判断同步记录是否已过期（超时未更新）
     * @param threshold 超时阈值时间点
     * @return true 如果 startedAt 早于阈值（视为崩溃）
     */
    public boolean isStale(LocalDateTime threshold) {
        return this.startedAt != null && this.startedAt.isBefore(threshold);
    }

    public void completeSuccess(int insertedCount, int updatedCount, int statusChangedCount,
                                 String reconciliationStatus, String reconciliationDetails) {
        this.status = "success";
        this.insertedCount = insertedCount;
        this.updatedCount = updatedCount;
        this.statusChangedCount = statusChangedCount;
        this.reconciliationStatus = reconciliationStatus;
        // JSON 列需要合法的 JSON 值，将字符串包装为 JSON 字符串值
        this.reconciliationDetails = reconciliationDetails != null
                ? "\"" + reconciliationDetails.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
                : null;
        this.finishedAt = LocalDateTime.now();
    }

    public void completeFailure(String errorMessage) {
        this.status = "failed";
        this.errorMessage = errorMessage;
        this.finishedAt = LocalDateTime.now();
    }
}
