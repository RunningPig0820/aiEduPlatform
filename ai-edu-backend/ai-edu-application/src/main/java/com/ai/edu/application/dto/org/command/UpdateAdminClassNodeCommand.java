package com.ai.edu.application.dto.org.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新行政班节点命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAdminClassNodeCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 节点名称 */
    private String name;

    /** 学段编码 */
    private String stageCode;

    /** 年制编码 */
    private String stageYearCode;

    /** 年级编码 */
    private String gradeCode;

    /** 入学年份 */
    private String enrollmentYear;

    /** 排序序号 */
    private Integer sortOrder;

    /** 描述 */
    private String description;
}
