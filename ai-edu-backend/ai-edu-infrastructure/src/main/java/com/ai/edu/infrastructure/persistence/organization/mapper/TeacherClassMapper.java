package com.ai.edu.infrastructure.persistence.organization.mapper;

import com.ai.edu.infrastructure.persistence.organization.po.TeacherClassPO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * 教师-班级关联Mapper接口
 * 操作持久化对象 TeacherClassPO
 */
@DS("org")
@Mapper
public interface TeacherClassMapper extends BaseMapper<TeacherClassPO> {

    @Select("SELECT * FROM t_teacher_class WHERE teacher_id = #{teacherId} AND class_id = #{classId} AND is_deleted = false")
    Optional<TeacherClassPO> selectByTeacherIdAndClassId(@Param("teacherId") Long teacherId, @Param("classId") Long classId);

    @Select("SELECT * FROM t_teacher_class WHERE class_id = #{classId} AND is_deleted = false")
    List<TeacherClassPO> selectByClassId(@Param("classId") Long classId);

    @Select("SELECT * FROM t_teacher_class WHERE class_id = #{classId} AND status = 'ACTIVE' AND is_deleted = false")
    List<TeacherClassPO> selectActiveByClassId(@Param("classId") Long classId);

    @Select("SELECT * FROM t_teacher_class WHERE teacher_id = #{teacherId} AND is_deleted = false")
    List<TeacherClassPO> selectByTeacherId(@Param("teacherId") Long teacherId);

    @Select("SELECT * FROM t_teacher_class WHERE teacher_id = #{teacherId} AND status = 'ACTIVE' AND is_deleted = false")
    List<TeacherClassPO> selectActiveByTeacherId(@Param("teacherId") Long teacherId);

    @Select("SELECT * FROM t_teacher_class WHERE class_id = #{classId} AND is_head_teacher = true AND status = 'ACTIVE' AND is_deleted = false")
    Optional<TeacherClassPO> selectHeadTeacherByClassId(@Param("classId") Long classId);

    @Select("SELECT * FROM t_teacher_class WHERE teacher_id = #{teacherId} AND subject = #{subject} AND is_deleted = false")
    List<TeacherClassPO> selectByTeacherIdAndSubject(@Param("teacherId") Long teacherId, @Param("subject") String subject);

    @Select("SELECT COUNT(*) > 0 FROM t_teacher_class WHERE teacher_id = #{teacherId} AND class_id = #{classId} AND is_deleted = false")
    boolean existsByTeacherIdAndClassId(@Param("teacherId") Long teacherId, @Param("classId") Long classId);

    @Select("SELECT COUNT(*) > 0 FROM t_teacher_class WHERE class_id = #{classId} AND is_head_teacher = true AND status = 'ACTIVE' AND is_deleted = false")
    boolean existsHeadTeacherByClassId(@Param("classId") Long classId);

    @Select("SELECT COUNT(*) FROM t_teacher_class WHERE class_id = #{classId} AND status = #{status} AND is_deleted = false")
    int countByClassIdAndStatus(@Param("classId") Long classId, @Param("status") String status);

    @Select("DELETE FROM t_teacher_class WHERE teacher_id = #{teacherId}")
    void deleteByTeacherId(@Param("teacherId") Long teacherId);

    @Select("DELETE FROM t_teacher_class WHERE class_id = #{classId}")
    void deleteByClassId(@Param("classId") Long classId);
}