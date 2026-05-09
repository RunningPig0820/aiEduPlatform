package com.ai.edu.application.dto.org;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 班级DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long schoolId;

    private String name;

    private String code;

    private String grade;

    private String schoolYear;

    private String classType;

    private String status;

    private String description;

    private Integer studentCount;

    private Long headTeacherId;
}