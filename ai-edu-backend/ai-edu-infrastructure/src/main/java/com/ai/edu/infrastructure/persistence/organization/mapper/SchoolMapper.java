package com.ai.edu.infrastructure.persistence.organization.mapper;

import com.ai.edu.infrastructure.persistence.organization.po.SchoolPO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学校Mapper接口
 * 操作持久化对象 SchoolPO
 */
@DS("org")
@Mapper
public interface SchoolMapper extends BaseMapper<SchoolPO> {

    @Select("SELECT * FROM t_school WHERE code = #{code} AND is_deleted = false")
    SchoolPO selectByCode(@Param("code") String code);

    @Select("SELECT * FROM t_school WHERE name = #{name} AND is_deleted = false")
    SchoolPO selectByName(@Param("name") String name);

    @Select("SELECT * FROM t_school WHERE province = #{province} AND city = #{city} AND is_deleted = false")
    List<SchoolPO> selectByProvinceAndCity(@Param("province") String province, @Param("city") String city);

    @Select("SELECT * FROM t_school WHERE school_type = #{schoolType} AND is_deleted = false")
    List<SchoolPO> selectByInstitutionalType(@Param("schoolType") String schoolType);

    @Select("SELECT * FROM t_school WHERE is_deleted = false")
    List<SchoolPO> selectAllActive();

    @Select("SELECT * FROM t_school")
    List<SchoolPO> selectAll();

    @Select("SELECT COUNT(*) > 0 FROM t_school WHERE code = #{code} AND is_deleted = false")
    boolean existsByCode(@Param("code") String code);

    @Select("SELECT COUNT(*) > 0 FROM t_school WHERE name = #{name} AND is_deleted = false")
    boolean existsByName(@Param("name") String name);
}