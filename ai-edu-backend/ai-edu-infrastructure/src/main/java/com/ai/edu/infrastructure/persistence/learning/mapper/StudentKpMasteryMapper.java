package com.ai.edu.infrastructure.persistence.learning.mapper;

import com.ai.edu.infrastructure.persistence.learning.po.StudentKpMasteryPo;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学生知识点掌握度 Mapper（路由 ai_edu_learning 库）。
 *
 * <p>UPSERT 幂等：student_id + kp_key 唯一（uk_student_kp），INSERT ... ON DUPLICATE KEY UPDATE。
 */
@Mapper
@DS("learning")
public interface StudentKpMasteryMapper extends BaseMapper<StudentKpMasteryPo> {

    /**
     * 掌握度 UPSERT（存在则更新：label/level/evidence/last_session；不存在则插入）。
     * 新建插入时经 useGeneratedKeys 回填主键。
     */
    @Insert("INSERT INTO t_student_kp_mastery (student_id, kp_key, kp_label, mastery_level, evidence, last_session_id, created_by, modified_by, is_deleted) " +
            "VALUES (#{studentId}, #{kpKey}, #{kpLabel}, #{masteryLevel}, #{evidence}, #{lastSessionId}, #{createdBy}, #{modifiedBy}, 0) " +
            "ON DUPLICATE KEY UPDATE kp_label = VALUES(kp_label), mastery_level = VALUES(mastery_level), " +
            "evidence = VALUES(evidence), last_session_id = VALUES(last_session_id), " +
            "updated_at = NOW(), modified_by = VALUES(modified_by)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int upsert(StudentKpMasteryPo po);

    /** 按学生 + 知识点 URI 查单条（读现值后取 max）。 */
    @Select("SELECT * FROM t_student_kp_mastery WHERE student_id = #{studentId} AND kp_key = #{kpKey} AND is_deleted = false LIMIT 1")
    StudentKpMasteryPo selectByStudentAndKp(@Param("studentId") Long studentId, @Param("kpKey") String kpKey);

    /** 按学生查全部掌握度（供知识图谱叠加）。 */
    @Select("SELECT * FROM t_student_kp_mastery WHERE student_id = #{studentId} AND is_deleted = false ORDER BY id")
    List<StudentKpMasteryPo> selectByStudentId(@Param("studentId") Long studentId);
}
