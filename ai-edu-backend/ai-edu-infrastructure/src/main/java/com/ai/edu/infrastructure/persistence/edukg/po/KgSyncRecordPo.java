package com.ai.edu.infrastructure.persistence.edukg.po;

import com.ai.edu.domain.edukg.model.entity.KgSyncRecord;
import com.ai.edu.infrastructure.persistence.edukg.util.EntityFactory;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识图谱-同步记录持久化对象
 */
@TableName("t_kg_sync_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KgSyncRecordPo {

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

    public static KgSyncRecordPo from(KgSyncRecord entity) {
        if (entity == null) return null;
        KgSyncRecordPo po = new KgSyncRecordPo();
        po.id = entity.getId();
        po.syncType = entity.getSyncType();
        po.scope = entity.getScope();
        po.status = entity.getStatus();
        po.insertedCount = entity.getInsertedCount();
        po.updatedCount = entity.getUpdatedCount();
        po.statusChangedCount = entity.getStatusChangedCount();
        po.reconciliationStatus = entity.getReconciliationStatus();
        po.reconciliationDetails = entity.getReconciliationDetails();
        po.errorMessage = entity.getErrorMessage();
        po.details = entity.getDetails();
        po.startedAt = entity.getStartedAt();
        po.finishedAt = entity.getFinishedAt();
        po.createdBy = entity.getCreatedBy();
        po.modifiedBy = entity.getModifiedBy();
        po.deleted = entity.getDeleted();
        return po;
    }

    public KgSyncRecord toEntity() {
        KgSyncRecord entity = EntityFactory.create(KgSyncRecord.class);
        entity.setId(this.id);
        entity.setSyncType(this.syncType);
        entity.setScope(this.scope);
        entity.setStatus(this.status);
        entity.setInsertedCount(this.insertedCount);
        entity.setUpdatedCount(this.updatedCount);
        entity.setStatusChangedCount(this.statusChangedCount);
        entity.setReconciliationStatus(this.reconciliationStatus);
        entity.setReconciliationDetails(this.reconciliationDetails);
        entity.setErrorMessage(this.errorMessage);
        entity.setDetails(this.details);
        entity.setStartedAt(this.startedAt);
        entity.setFinishedAt(this.finishedAt);
        entity.setCreatedBy(this.createdBy);
        entity.setModifiedBy(this.modifiedBy);
        entity.setDeleted(this.deleted);
        return entity;
    }

    public static List<KgSyncRecordPo> fromList(List<KgSyncRecord> entities) {
        if (entities == null) return null;
        return entities.stream().map(KgSyncRecordPo::from).collect(Collectors.toList());
    }

    public static List<KgSyncRecord> toEntityList(List<KgSyncRecordPo> pos) {
        if (pos == null) return null;
        return pos.stream().map(KgSyncRecordPo::toEntity).collect(Collectors.toList());
    }
}
