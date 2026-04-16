package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 同步请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String subject;
    private String phase;
    private String grade;
    private String textbookUri;
}
