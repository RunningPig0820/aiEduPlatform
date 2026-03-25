package com.ai.edu.infrastructure.persistence.mapper;

import com.ai.edu.domain.organization.model.entity.TeacherClass;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * 老师-班级关联Mapper接口
 */
@Mapper
public interface TeacherClassMapper extends BaseMapper<TeacherClass> {

    @Select("SELECT * FROM t_teacher_class WHERE teacher_id = #{teacherId} AND class_id = #{classId} AND is_deleted = false")
    Optional<TeacherClass> selectByTeacherIdAndClassId(@Param("teacherId") Long teacherId, @Param("classId") Long classId);

    @Select("SELECT * FROM t_teacher_class WHERE class_id = #{classId} AND is_deleted = false")
    List<TeacherClass> selectByClassId(@Param("classId") Long classId);

    @Select("SELECT * FROM t_teacher_class WHERE class_id = #{classId} AND status = 'ACTIVE' AND is_deleted = false")
    List<TeacherClass> selectActiveByClassId(@Param("classId") Long classId);

    @Select("SELECT * FROM t_teacher_class WHERE teacher_id = #{teacherId} AND is_deleted = false")
    List<TeacherClass> selectByTeacherId(@Param("teacherId") Long teacherId);

    @Select("SELECT * FROM t_teacher_class WHERE teacher_id = #{teacherId} AND status = 'ACTIVE' AND is_deleted = false")
    List<TeacherClass> selectActiveByTeacherId(@Param("teacherId") Long teacherId);

    @Select("SELECT * FROM t_teacher_class WHERE class_id = #{classId} AND is_head_teacher = true AND status = 'ACTIVE' AND is_deleted = false")
    Optional<TeacherClass> selectHeadTeacherByClassId(@Param("classId") Long classId);

    @Select("SELECT * FROM t_teacher_class WHERE teacher_id = #{teacherId} AND subject = #{subject} AND is_deleted = false")
    List<TeacherClass> selectByTeacherIdAndSubject(@Param("teacherId") Long teacherId, @Param("subject") String subject);

    @Select("SELECT COUNT(*) > 0 FROM t_teacher_class WHERE teacher_id = #{teacherId} AND class_id = #{classId} AND is_deleted = false")
    boolean existsByTeacherIdAndClassId(@Param("teacherId") Long teacherId, @Param("classId") Long classId);

    @Select("SELECT COUNT(*) > 0 FROM t_teacher_class WHERE class_id = #{classId} AND is_head_teacher = true AND status = 'ACTIVE' AND is_deleted = false")
    boolean existsHeadTeacherByClassId(@Param("classId") Long classId);

    @Select("SELECT COUNT(*) FROM t_teacher_class WHERE class_id = #{classId} AND status = #{status} AND is_deleted = false")
    int countByClassIdAndStatus(@Param("classId") Long classId, @Param("status") String status);
}