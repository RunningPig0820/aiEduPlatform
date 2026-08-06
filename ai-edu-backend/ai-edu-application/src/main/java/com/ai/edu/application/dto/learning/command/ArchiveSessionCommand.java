package com.ai.edu.application.dto.learning.command;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 主动结束会话命令（end_reason=ABANDONED，掌握度不提升 + COS 终态写）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveSessionCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "会话ID不能为空")
    private Long sessionId;
}
