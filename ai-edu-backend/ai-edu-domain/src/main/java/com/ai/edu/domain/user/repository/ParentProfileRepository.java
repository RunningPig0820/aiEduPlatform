package com.ai.edu.domain.user.repository;

import com.ai.edu.domain.user.model.entity.ParentProfile;

import java.util.List;

/**
 * 家长信息仓储接口
 */
public interface ParentProfileRepository {

    /**
     * 保存（新增或更新）
     */
    ParentProfile save(ParentProfile profile);

    /**
     * 批量保存
     */
    List<ParentProfile> saveAll(List<ParentProfile> profiles);

    /**
     * 根据学生用户ID查询所有绑定的家长关联
     */
    List<ParentProfile> findByStudentUserId(Long studentUserId);

    /**
     * 根据家长用户ID查询所有绑定的学生关联
     */
    List<ParentProfile> findByParentUserId(Long parentUserId);

    /**
     * 根据学生用户ID删除所有关联
     */
    void deleteByStudentUserId(Long studentUserId);
}
