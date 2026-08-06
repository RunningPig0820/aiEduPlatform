package com.ai.edu.application.dto.learning.command;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 发起答疑会话命令（student_id 取自 HttpSession，请求体只带消息文本）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartTutoringCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 题目/消息文本（OCR 确认后或手打粘贴） */
    @NotBlank(message = "消息不能为空")
    private String message;
}
