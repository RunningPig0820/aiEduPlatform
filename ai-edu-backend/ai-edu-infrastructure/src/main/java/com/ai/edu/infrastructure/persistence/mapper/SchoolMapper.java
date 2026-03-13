package com.ai.edu.infrastructure.persistence.mapper;

import com.ai.edu.domain.organization.model.entity.School;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学校Mapper接口
 */
@Mapper
public interface SchoolMapper extends BaseMapper<School> {

    @Select("SELECT * FROM t_school WHERE code = #{code} AND is_deleted = false")
    School selectByCode(String code);

    @Select("SELECT * FROM t_school WHERE province = #{province} AND city = #{city} AND is_deleted = false")
    List<School> selectByProvinceAndCity(String province, String city);

    @Select("SELECT * FROM t_school WHERE school_type = #{schoolType} AND is_deleted = false")
    List<School> selectBySchoolType(String schoolType);

    @Select("SELECT * FROM t_school WHERE is_deleted = false")
    List<School> selectAllActive();

    @Select("SELECT COUNT(*) > 0 FROM t_school WHERE code = #{code} AND is_deleted = false")
    boolean existsByCode(String code);
}