package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 同步记录 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncRecordDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String syncType;
    private String scope;
    private String status;
    private int insertedCount;
    private int updatedCount;
    private int statusChangedCount;
    private String reconciliationStatus;
    private String errorMessage;
    private String startedAt;
    private String finishedAt;
}
