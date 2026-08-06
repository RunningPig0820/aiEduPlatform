package com.ai.edu.application.dto.learning.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 发送学生回答/消息命令（追加到会话，触发 decide→guard→generate）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    @NotBlank(message = "消息不能为空")
    private String content;
}
