package com.ai.edu.application.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 挂起确认响应（POST /api/kg/aliases/pending/{id}/confirm）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmKpAliasDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 是否更新 */
    private boolean updated;

    /** 更新后观测状态 */
    private String status;
}
