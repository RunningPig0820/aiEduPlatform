package com.ai.edu.application.dto.learning.command;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 请求答案命令（第 1 次思路 / 第 2 次答案，答案护栏放行）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestAnswerCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "会话ID不能为空")
    private Long sessionId;
}
