package com.ai.edu.domain.organization.repository;

import com.ai.edu.domain.organization.model.entity.OrgTeacher;
import com.ai.edu.domain.organization.model.valueobject.OrgTeacherId;
import com.ai.edu.domain.organization.model.valueobject.OrgTeacherQueryParam;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.Optional;

/**
 * 教职工仓储接口
 */
public interface OrgTeacherRepository {

    /**
     * 保存教职工关联关系
     */
    OrgTeacher save(OrgTeacher orgTeacher);

    /**
     * 根据ID查询教职工关联关系
     */
    Optional<OrgTeacher> findById(OrgTeacherId id);

    /**
     * 根据学校ID查询所有教职工关联关系
     */
    List<OrgTeacher> findBySchoolId(SchoolId schoolId);

    /**
     * 根据部门ID查询所有教职工关联关系
     */
    List<OrgTeacher> findByDepartmentId(Long departmentId);

    /**
     * 根据学校ID和用户ID查询教职工关联关系（用于唯一性校验）
     */
    Optional<OrgTeacher> findBySchoolIdAndUserId(SchoolId schoolId, Long userId);

    /**
     * 分页查询教职工关联关系
     */
    IPage<OrgTeacher> queryPage(OrgTeacherQueryParam param, int pageNum, int pageSize);

    /**
     * 根据ID删除教职工关联关系（逻辑删除）
     */
    void deleteById(OrgTeacherId id);

    /**
     * 根据ID列表批量查询教职工关联关系
     */
    List<OrgTeacher> findByIds(List<OrgTeacherId> ids);
}