package com.ai.edu.infrastructure.persistence.learning.mapper;

import com.ai.edu.infrastructure.persistence.learning.po.ErrorEventPo;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 答疑错误事件 Mapper（路由 ai_edu_learning 库）。
 */
@Mapper
@DS("learning")
public interface ErrorEventMapper extends BaseMapper<ErrorEventPo> {

    /** 按学生查错误历史/趋势。 */
    @Select("SELECT * FROM t_tutoring_error_event WHERE student_id = #{studentId} AND is_deleted = false ORDER BY id")
    List<ErrorEventPo> selectByStudentId(@Param("studentId") Long studentId);
}
