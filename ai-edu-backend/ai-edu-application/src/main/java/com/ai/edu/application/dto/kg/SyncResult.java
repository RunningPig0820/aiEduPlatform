package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 同步结果 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long syncId;
    private String status;
    private int insertedCount;
    private int updatedCount;
    private int statusChangedCount;
    private String reconciliationStatus;
    private long duration;
}
