package com.ai.edu.domain.organization.repository;

import com.ai.edu.domain.organization.model.entity.SchoolUserAssociation;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import com.ai.edu.domain.shared.valueobject.UserId;

import java.util.List;
import java.util.Optional;

/**
 * 学校用户关联仓储接口
 * 定义用户与学校关联关系的持久化操作
 */
public interface SchoolUserRepository {

    /**
     * 保存学校用户关联
     */
    SchoolUserAssociation save(SchoolUserAssociation association);

    /**
     * 根据ID查找关联
     */
    Optional<SchoolUserAssociation> findById(Long id);

    /**
     * 根据用户ID查找所有关联
     */
    List<SchoolUserAssociation> findByUserId(UserId userId);

    /**
     * 根据学校ID查找所有关联
     */
    List<SchoolUserAssociation> findBySchoolId(SchoolId schoolId);

    /**
     * 根据学校ID和用户ID查找关联
     */
    Optional<SchoolUserAssociation> findBySchoolIdAndUserId(SchoolId schoolId, UserId userId);

    /**
     * 检查学校用户关联是否存在
     */
    boolean existsBySchoolIdAndUserId(SchoolId schoolId, UserId userId);

    /**
     * 根据ID删除关联
     */
    void deleteById(Long id);

    /**
     * 根据学校ID和用户ID删除关联
     */
    void deleteBySchoolIdAndUserId(SchoolId schoolId, UserId userId);

    /**
     * 根据学校ID删除所有关联
     */
    void deleteBySchoolId(SchoolId schoolId);

    /**
     * 统计学校的用户数量
     */
    long countBySchoolId(SchoolId schoolId);

    /**
     * 统计用户的学校数量
     */
    long countByUserId(UserId userId);
}