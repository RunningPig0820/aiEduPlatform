package com.ai.edu.application.dto.org.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新行政班学生命令
 *
 * 组织域只支持修改学号和状态，学生用户基本信息在用户中心修改。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAdminClassStudentCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * StudentClass 关联关系ID
     */
    private Long id;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 状态：GRADUATED（毕业）/ TRANSFERRED（转出）/ ACTIVE（恢复在读）
     */
    private String status;
}
