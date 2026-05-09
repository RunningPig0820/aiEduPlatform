package com.ai.edu.infrastructure.persistence.organization.mapper;

import com.ai.edu.infrastructure.persistence.organization.po.GradePO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * 年级Mapper接口
 * 操作持久化对象 GradePO
 */
@DS("org")
@Mapper
public interface GradeMapper extends BaseMapper<GradePO> {

    @Select("SELECT * FROM t_grade WHERE code = #{code} AND is_deleted = false")
    Optional<GradePO> selectByCode(@Param("code") String code);

    @Select("SELECT * FROM t_grade WHERE school_id = #{schoolId} AND is_deleted = false")
    List<GradePO> selectBySchoolId(@Param("schoolId") Long schoolId);

    @Select("SELECT * FROM t_grade WHERE grade_level = #{gradeLevel} AND is_deleted = false")
    List<GradePO> selectByGradeLevel(@Param("gradeLevel") Integer gradeLevel);

    @Select("SELECT * FROM t_grade WHERE is_deleted = false")
    List<GradePO> selectAllActive();

    @Select("SELECT COUNT(*) > 0 FROM t_grade WHERE code = #{code} AND is_deleted = false")
    boolean existsByCode(@Param("code") String code);
}