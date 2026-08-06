package com.ai.edu.domain.learning.model.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * OCR 识别结果（Python /api/ocr/recognize 响应，{text, confidence}）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 识别出的题目文本 */
    private String text;

    /** 识别置信度 0-1 */
    private Double confidence;
}
