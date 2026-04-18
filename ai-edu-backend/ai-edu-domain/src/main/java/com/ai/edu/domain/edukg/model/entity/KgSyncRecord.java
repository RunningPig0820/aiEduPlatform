package com.ai.edu.domain.edukg.model.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    public static KgSyncRecord create(String syncType, String scope, Long createdBy) {
        KgSyncRecord record = new KgSyncRecord();
        record.syncType = syncType;
        record.scope = scope;
        record.status = "running";
        record.createdBy = createdBy;
        record.startedAt = LocalDateTime.now();
        return record;
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
