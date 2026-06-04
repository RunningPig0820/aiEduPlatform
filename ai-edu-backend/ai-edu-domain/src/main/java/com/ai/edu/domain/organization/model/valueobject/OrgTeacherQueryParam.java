package com.ai.edu.domain.organization.model.valueobject;

import lombok.Builder;
import lombok.Getter;

/**
 * 教职工查询参数值对象
 */
@Getter
@Builder
public class OrgTeacherQueryParam {

    private Long schoolId;      // 学校ID
    private Long departmentId;  // 部门ID
    private Long userId;        // 用户ID

    public boolean hasSchoolId() {
        return schoolId != null && schoolId > 0;
    }

    public boolean hasDepartmentId() {
        return departmentId != null && departmentId > 0;
    }

    public boolean hasUserId() {
        return userId != null && userId > 0;
    }
}