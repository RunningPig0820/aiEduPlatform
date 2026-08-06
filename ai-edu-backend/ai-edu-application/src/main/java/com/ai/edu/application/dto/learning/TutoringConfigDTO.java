package com.ai.edu.application.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 答疑前端能力配置（GET /api/tutoring/config）——前端据此显示/隐藏拍照入口。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutoringConfigDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 拍题 OCR 开关（false 时前端隐藏拍照入口，仅手打/粘贴题目） */
    private Boolean ocrEnabled;
}
