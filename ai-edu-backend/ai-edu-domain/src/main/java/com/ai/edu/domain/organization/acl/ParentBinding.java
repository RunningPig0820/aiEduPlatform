package com.ai.edu.domain.organization.acl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 家长绑定参数（防腐层简单 VO）
 *
 * 用于 Gateway 方法参数，在组织域和用户域之间传递家长绑定信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentBinding {

    /**
     * 家长用户ID
     */
    private Long parentUserId;

    /**
     * 与学生关系类型（父亲/母亲/监护人等）
     */
    private String relationship;
}
