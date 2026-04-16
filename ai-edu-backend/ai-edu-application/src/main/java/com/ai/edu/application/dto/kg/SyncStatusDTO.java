package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 同步状态 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncStatusDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String status;
    private String lastSyncAt;
    private String lastSyncStatus;
    private int lastInsertedCount;
    private int lastUpdatedCount;
    private String lastReconciliationStatus;
}
