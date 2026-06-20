package com.ai.edu.application.dto.org;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 行政班学生响应DTO（聚合返回完整信息）
 *
 * 包含：StudentClass 关联信息 + 学生用户基本信息（含脱敏身份证）+ 绑定家长列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminClassStudentDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * StudentClass 关联关系ID
     */
    private Long id;

    /**
     * 学生用户ID（用户域）
     */
    private Long studentUserId;

    /**
     * 学生姓名（来自用户域）
     */
    private String name;

    /**
     * 学生手机号（来自用户域）
     */
    private String phone;

    /**
     * 脱敏后的身份证号（如 "110101****1234"）
     */
    private String maskedIdCard;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 行政班班级节点ID（Department.id）
     */
    private Long deptId;

    /**
     * 行政班班级名称
     */
    private String deptName;

    /**
     * 入学日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate joinDate;

    /**
     * 状态：ACTIVE / GRADUATED / TRANSFERRED
     */
    private String status;

    /**
     * 绑定的家长列表
     */
    private List<ParentInfoDTO> parents;

    /**
     * 家长信息（嵌套DTO）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParentInfoDTO implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 家长用户ID
         */
        private Long userId;

        /**
         * 家长姓名
         */
        private String name;

        /**
         * 家长手机号
         */
        private String phone;

        /**
         * 关系类型（父亲/母亲/监护人等）
         */
        private String relationship;
    }
}
