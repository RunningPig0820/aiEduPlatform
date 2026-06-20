package com.ai.edu.domain.user.model.entity;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 家长信息实体
 *
 * 存储学生与家长的关联关系，位于用户域。
 * 基于 t_parent_profile 表，一个学生可以绑定多个家长，一个家长可以关联多个学生。
 */
@Getter
public class ParentProfile {

    private Long id;
    private Long studentUserId;
    private Long parentUserId;
    private String relationship;
    private Boolean isPrimary;
    private Long createdBy;
    private Long modifiedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;

    protected ParentProfile() {}

    /**
     * 创建家长-学生关联
     */
    public static ParentProfile create(Long studentUserId, Long parentUserId, String relationship) {
        ParentProfile profile = new ParentProfile();
        profile.studentUserId = studentUserId;
        profile.parentUserId = parentUserId;
        profile.relationship = relationship;
        profile.isPrimary = false;
        profile.createdBy = 0L;
        profile.modifiedBy = 0L;
        profile.createdAt = LocalDateTime.now();
        profile.updatedAt = LocalDateTime.now();
        profile.deleted = false;
        return profile;
    }

    /**
     * 从持久化对象重建实体
     */
    public static ParentProfile fromPO(Long id, Long studentUserId, Long parentUserId,
                                        String relationship, Boolean isPrimary,
                                        Long createdBy, Long modifiedBy,
                                        LocalDateTime createdAt, LocalDateTime updatedAt,
                                        boolean deleted) {
        ParentProfile profile = new ParentProfile();
        profile.id = id;
        profile.studentUserId = studentUserId;
        profile.parentUserId = parentUserId;
        profile.relationship = relationship;
        profile.isPrimary = isPrimary != null && isPrimary;
        profile.createdBy = createdBy;
        profile.modifiedBy = modifiedBy;
        profile.createdAt = createdAt;
        profile.updatedAt = updatedAt;
        profile.deleted = deleted;
        return profile;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setAsPrimary() {
        this.isPrimary = true;
    }
}
