package com.ai.edu.infrastructure.persistence.organization.mapper;

import com.ai.edu.infrastructure.persistence.organization.po.DepartmentEduPO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 教育部门扩展属性 Mapper
 */
@DS("org")
@Mapper
public interface DepartmentEduMapper extends BaseMapper<DepartmentEduPO> {

    /**
     * 根据部门ID查询扩展属性
     */
    @Select("SELECT * FROM t_department_edu WHERE dept_id = #{deptId} AND is_deleted = 0")
    DepartmentEduPO selectByDeptId(@Param("deptId") Long deptId);

    /**
     * 根据学校ID查询所有扩展属性
     */
    @Select("SELECT * FROM t_department_edu WHERE school_id = #{schoolId} AND is_deleted = 0 ORDER BY dept_id")
    List<DepartmentEduPO> selectBySchoolId(@Param("schoolId") Long schoolId);

    /**
     * 根据部门ID逻辑删除
     */
    @Select("UPDATE t_department_edu SET is_deleted = 1 WHERE dept_id = #{deptId} AND is_deleted = 0")
    int deleteByDeptId(@Param("deptId") Long deptId);
}
