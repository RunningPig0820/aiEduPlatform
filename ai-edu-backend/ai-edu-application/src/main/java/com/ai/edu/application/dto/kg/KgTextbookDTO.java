package com.ai.edu.application.dto.kg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 教材 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KgTextbookDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String uri;
    private String label;
    private String grade;
    private String stage;
    private String edition;
    private Integer orderIndex;
    private String subject;
    private String status;
}
