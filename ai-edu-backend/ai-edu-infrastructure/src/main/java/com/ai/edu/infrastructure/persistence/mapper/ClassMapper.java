package com.ai.edu.infrastructure.persistence.mapper;

import com.ai.edu.domain.organization.model.entity.Class;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * 班级Mapper接口
 */
@Mapper
public interface ClassMapper extends BaseMapper<Class> {

    @Select("SELECT * FROM t_class WHERE code = #{code} AND is_deleted = false")
    Optional<Class> selectByCode(String code);

    @Select("SELECT * FROM t_class WHERE school_id = #{schoolId} AND is_deleted = false")
    List<Class> selectBySchoolId(Long schoolId);

    @Select("SELECT * FROM t_class WHERE grade = #{grade} AND is_deleted = false")
    List<Class> selectByGrade(String grade);

    @Select("SELECT * FROM t_class WHERE school_year = #{schoolYear} AND is_deleted = false")
    List<Class> selectBySchoolYear(String schoolYear);

    @Select("SELECT * FROM t_class WHERE status = #{status} AND is_deleted = false")
    List<Class> selectByStatus(String status);

    @Select("SELECT * FROM t_class WHERE school_id = #{schoolId} AND status = 'ACTIVE' AND is_deleted = false")
    List<Class> selectActiveBySchoolId(Long schoolId);

    @Select("SELECT * FROM t_class WHERE id = #{id} AND status = 'ACTIVE' AND is_deleted = false")
    Optional<Class> selectActiveById(Long id);

    @Select("SELECT COUNT(*) > 0 FROM t_class WHERE name = #{name} AND school_year = #{schoolYear} AND is_deleted = false")
    boolean existsByNameAndSchoolYear(@Param("name") String name, @Param("schoolYear") String schoolYear);

    @Select("SELECT COUNT(*) FROM t_class WHERE school_id = #{schoolId} AND status = #{status} AND is_deleted = false")
    int countBySchoolIdAndStatus(@Param("schoolId") Long schoolId, @Param("status") String status);
}