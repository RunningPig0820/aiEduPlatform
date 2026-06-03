package com.ai.edu.infrastructure.persistence.organization.mapper;

import com.ai.edu.infrastructure.persistence.organization.po.DepartmentPO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 部门Mapper接口
 * 操作持久化对象 DepartmentPO
 */
@DS("org")
@Mapper
public interface DepartmentMapper extends BaseMapper<DepartmentPO> {

    /**
     * 根据学校ID和名称查询部门
     */
    @Select("SELECT * FROM t_department WHERE school_id = #{schoolId} AND name = #{name} AND is_deleted = false")
    DepartmentPO selectBySchoolIdAndName(@Param("schoolId") Long schoolId, @Param("name") String name);

    /**
     * 查询学校的所有部门
     */
    @Select("SELECT * FROM t_department WHERE school_id = #{schoolId} AND is_deleted = false ORDER BY sort_order, id")
    List<DepartmentPO> selectBySchoolId(@Param("schoolId") Long schoolId);

    /**
     * 查询学校的根部门
     */
    @Select("SELECT * FROM t_department WHERE school_id = #{schoolId} AND parent_id IS NULL AND is_deleted = false ORDER BY sort_order, id")
    List<DepartmentPO> selectRootDepartments(@Param("schoolId") Long schoolId);

    /**
     * 查询直接子部门
     */
    @Select("SELECT * FROM t_department WHERE parent_id = #{parentId} AND is_deleted = false ORDER BY sort_order, id")
    List<DepartmentPO> selectChildren(@Param("parentId") Long parentId);

    /**
     * 查询所有子孙部门（通过 department_path）
     * department_path 包含自己的ID，所以子孙部门的路径以 "path_" 开头
     * 例如：查询 path="1_2" 的子孙部门，匹配 "1_2_%"
     */
    @Select("SELECT * FROM t_department WHERE department_path LIKE #{pathPattern} AND is_deleted = false ORDER BY department_path, sort_order")
    List<DepartmentPO> selectDescendants(@Param("pathPattern") String pathPattern);

    /**
     * 检查学校下部门名称是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM t_department WHERE school_id = #{schoolId} AND name = #{name} AND is_deleted = false")
    boolean existsBySchoolIdAndName(@Param("schoolId") Long schoolId, @Param("name") String name);

    /**
     * 检查是否有子部门
     */
    @Select("SELECT COUNT(*) > 0 FROM t_department WHERE parent_id = #{id} AND is_deleted = false")
    boolean hasChildren(@Param("id") Long id);

    /**
     * 批量更新子孙部门的路径
     * 将 oldPath 开头的路径替换为 newPath
     */
    @Update("UPDATE t_department SET department_path = REPLACE(department_path, #{oldPath}, #{newPath}) " +
            "WHERE department_path LIKE #{oldPathPattern} AND is_deleted = false")
    int updateDescendantsPath(@Param("oldPath") String oldPath, @Param("newPath") String newPath, @Param("oldPathPattern") String oldPathPattern);
}