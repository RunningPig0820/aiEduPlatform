package com.ai.edu.infrastructure.persistence.organization.mapper;

import com.ai.edu.infrastructure.persistence.organization.po.ClassPO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * 班级Mapper接口
 * 操作持久化对象 ClassPO
 */
@DS("org")
@Mapper
public interface ClassMapper extends BaseMapper<ClassPO> {

    @Select("SELECT * FROM t_class WHERE code = #{code} AND is_deleted = false")
    Optional<ClassPO> selectByCode(@Param("code") String code);

    @Select("SELECT * FROM t_class WHERE school_id = #{schoolId} AND is_deleted = false")
    List<ClassPO> selectBySchoolId(@Param("schoolId") Long schoolId);

    @Select("SELECT * FROM t_class WHERE grade = #{grade} AND is_deleted = false")
    List<ClassPO> selectByGrade(@Param("grade") String grade);

    @Select("SELECT * FROM t_class WHERE school_year = #{schoolYear} AND is_deleted = false")
    List<ClassPO> selectBySchoolYear(@Param("schoolYear") String schoolYear);

    @Select("SELECT * FROM t_class WHERE status = #{status} AND is_deleted = false")
    List<ClassPO> selectByStatus(@Param("status") String status);

    @Select("SELECT * FROM t_class WHERE school_id = #{schoolId} AND status = 'ACTIVE' AND is_deleted = false")
    List<ClassPO> selectActiveBySchoolId(@Param("schoolId") Long schoolId);

    @Select("SELECT * FROM t_class WHERE id = #{id} AND status = 'ACTIVE' AND is_deleted = false")
    Optional<ClassPO> selectActiveById(@Param("id") Long id);

    @Select("SELECT COUNT(*) > 0 FROM t_class WHERE name = #{name} AND school_year = #{schoolYear} AND is_deleted = false")
    boolean existsByNameAndSchoolYear(@Param("name") String name, @Param("schoolYear") String schoolYear);

    @Select("SELECT COUNT(*) FROM t_class WHERE school_id = #{schoolId} AND status = #{status} AND is_deleted = false")
    int countBySchoolIdAndStatus(@Param("schoolId") Long schoolId, @Param("status") String status);
}