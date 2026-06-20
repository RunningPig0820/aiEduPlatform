package com.ai.edu.infrastructure.persistence.user.mapper;

import com.ai.edu.infrastructure.persistence.user.po.ParentProfilePO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 家长信息Mapper接口
 */
@DS("user")
@Mapper
public interface ParentProfileMapper extends BaseMapper<ParentProfilePO> {

    @Select("SELECT * FROM t_parent_profile WHERE student_user_id = #{studentUserId} AND is_deleted = 0 ORDER BY is_primary DESC, created_at ASC")
    List<ParentProfilePO> selectByStudentUserId(@Param("studentUserId") Long studentUserId);

    @Select("SELECT * FROM t_parent_profile WHERE parent_user_id = #{parentUserId} AND is_deleted = 0 ORDER BY created_at ASC")
    List<ParentProfilePO> selectByParentUserId(@Param("parentUserId") Long parentUserId);

    @Select("UPDATE t_parent_profile SET is_deleted = 1 WHERE student_user_id = #{studentUserId}")
    void deleteByStudentUserId(@Param("studentUserId") Long studentUserId);
}
