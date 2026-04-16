package com.ai.edu.domain.edukg.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识图谱-同步记录实体
 */
@TableName("t_kg_sync_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgSyncRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("sync_type")
    private String syncType;

    @TableField("scope")
    private String scope;

    @TableField("status")
    private String status;

    @TableField("inserted_count")
    private Integer insertedCount = 0;

    @TableField("updated_count")
    private Integer updatedCount = 0;

    @TableField("status_changed_count")
    private Integer statusChangedCount = 0;

    @TableField("reconciliation_status")
    private String reconciliationStatus;

    @TableField("reconciliation_details")
    private String reconciliationDetails;

    @TableField("error_message")
    private String errorMessage;

    @TableField("details")
    private String details;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;

    @TableField("created_by")
    private Long createdBy = 0L;

    @TableField("modified_by")
    private Long modifiedBy = 0L;

    @TableField("is_deleted")
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
        this.reconciliationDetails = reconciliationDetails;
        this.finishedAt = LocalDateTime.now();
    }

    public void completeFailure(String errorMessage) {
        this.status = "failed";
        this.errorMessage = errorMessage;
        this.finishedAt = LocalDateTime.now();
    }
}
