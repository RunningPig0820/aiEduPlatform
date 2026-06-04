package com.ai.edu.infrastructure.persistence.organization.mapper;

import com.ai.edu.infrastructure.persistence.organization.po.OrgTeacherPO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 教职工Mapper接口
 * 操作持久化对象 OrgTeacherPO
 */
@DS("org")
@Mapper
public interface OrgTeacherMapper extends BaseMapper<OrgTeacherPO> {

    /**
     * 根据学校ID查询所有教职工关联关系
     */
    @Select("SELECT * FROM t_org_teacher WHERE school_id = #{schoolId} AND is_deleted = false ORDER BY created_at DESC")
    List<OrgTeacherPO> selectBySchoolId(@Param("schoolId") Long schoolId);

    /**
     * 根据部门ID查询所有教职工关联关系
     */
    @Select("SELECT * FROM t_org_teacher WHERE department_id = #{departmentId} AND is_deleted = false ORDER BY created_at DESC")
    List<OrgTeacherPO> selectByDepartmentId(@Param("departmentId") Long departmentId);

    /**
     * 根据学校ID和用户ID查询教职工关联关系（用于唯一性校验）
     */
    @Select("SELECT * FROM t_org_teacher WHERE school_id = #{schoolId} AND user_id = #{userId} AND is_deleted = false")
    OrgTeacherPO selectBySchoolIdAndUserId(@Param("schoolId") Long schoolId, @Param("userId") Long userId);
}