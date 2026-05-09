package com.ai.edu.infrastructure.persistence.organization.mapper;

import com.ai.edu.infrastructure.persistence.organization.po.StudentClassPO;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * 学生-班级关联Mapper接口
 * 操作持久化对象 StudentClassPO
 */
@DS("org")
@Mapper
public interface StudentClassMapper extends BaseMapper<StudentClassPO> {

    @Select("SELECT * FROM t_student_class WHERE student_id = #{studentId} AND class_id = #{classId} AND is_deleted = false")
    Optional<StudentClassPO> selectByStudentIdAndClassId(@Param("studentId") Long studentId, @Param("classId") Long classId);

    @Select("SELECT * FROM t_student_class WHERE student_id = #{studentId} AND status = 'ACTIVE' AND is_deleted = false ORDER BY join_date DESC LIMIT 1")
    Optional<StudentClassPO> selectActiveByStudentId(@Param("studentId") Long studentId);

    @Select("SELECT * FROM t_student_class WHERE class_id = #{classId} AND is_deleted = false")
    List<StudentClassPO> selectByClassId(@Param("classId") Long classId);

    @Select("SELECT * FROM t_student_class WHERE class_id = #{classId} AND status = 'ACTIVE' AND is_deleted = false")
    List<StudentClassPO> selectActiveByClassId(@Param("classId") Long classId);

    @Select("SELECT * FROM t_student_class WHERE student_id = #{studentId} AND is_deleted = false")
    List<StudentClassPO> selectByStudentId(@Param("studentId") Long studentId);

    @Select("SELECT * FROM t_student_class WHERE status = #{status} AND is_deleted = false")
    List<StudentClassPO> selectByStatus(@Param("status") String status);

    @Select("SELECT COUNT(*) > 0 FROM t_student_class WHERE student_id = #{studentId} AND class_id = #{classId} AND is_deleted = false")
    boolean existsByStudentIdAndClassId(@Param("studentId") Long studentId, @Param("classId") Long classId);

    @Select("SELECT COUNT(*) FROM t_student_class WHERE class_id = #{classId} AND status = #{status} AND is_deleted = false")
    int countByClassIdAndStatus(@Param("classId") Long classId, @Param("status") String status);

    @Select("DELETE FROM t_student_class WHERE student_id = #{studentId}")
    void deleteByStudentId(@Param("studentId") Long studentId);

    @Select("DELETE FROM t_student_class WHERE class_id = #{classId}")
    void deleteByClassId(@Param("classId") Long classId);
}