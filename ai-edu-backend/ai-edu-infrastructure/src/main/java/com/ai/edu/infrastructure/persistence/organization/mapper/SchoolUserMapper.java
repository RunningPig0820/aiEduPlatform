package com.ai.edu.infrastructure.persistence.organization.mapper;

import com.ai.edu.infrastructure.persistence.organization.po.SchoolUserPO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

/**
 * 学校用户关联Mapper接口
 */
@DS("org")
@Mapper
public interface SchoolUserMapper extends BaseMapper<SchoolUserPO> {

    @Select("SELECT * FROM t_school_user WHERE user_id = #{userId} AND is_deleted = false")
    List<SchoolUserPO> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM t_school_user WHERE school_id = #{schoolId} AND is_deleted = false")
    List<SchoolUserPO> selectBySchoolId(@Param("schoolId") Long schoolId);

    @Select("SELECT * FROM t_school_user WHERE school_id = #{schoolId} AND user_id = #{userId} AND is_deleted = false")
    SchoolUserPO selectBySchoolIdAndUserId(@Param("schoolId") Long schoolId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) > 0 FROM t_school_user WHERE school_id = #{schoolId} AND user_id = #{userId} AND is_deleted = false")
    boolean existsBySchoolIdAndUserId(@Param("schoolId") Long schoolId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM t_school_user WHERE school_id = #{schoolId} AND is_deleted = false")
    long countBySchoolId(@Param("schoolId") Long schoolId);

    @Select("SELECT COUNT(*) FROM t_school_user WHERE user_id = #{userId} AND is_deleted = false")
    long countByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM t_school_user WHERE school_id = #{schoolId} AND user_id = #{userId}")
    void deleteBySchoolIdAndUserId(@Param("schoolId") Long schoolId, @Param("userId") Long userId);

    @Delete("DELETE FROM t_school_user WHERE school_id = #{schoolId}")
    void deleteBySchoolId(@Param("schoolId") Long schoolId);
}