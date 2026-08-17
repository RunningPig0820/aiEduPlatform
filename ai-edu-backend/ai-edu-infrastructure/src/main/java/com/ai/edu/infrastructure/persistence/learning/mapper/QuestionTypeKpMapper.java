package com.ai.edu.infrastructure.persistence.learning.mapper;

import com.ai.edu.infrastructure.persistence.learning.po.QuestionTypeKpPo;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 题型↔知识点 年级分布桶 Mapper（路由 ai_edu_learning 库）。
 *
 * <p>UPSERT 幂等：question_type_id + kp_uri 唯一（uk_type_kp）。
 */
@Mapper
@DS("learning")
public interface QuestionTypeKpMapper extends BaseMapper<QuestionTypeKpPo> {

    /**
     * 分布桶 UPSERT（同题型同知识点唯一，命中则更新统计与占比）。
     */
    @Insert("INSERT INTO t_kp_question_type_kp (question_type_id, kp_uri, grade_range, hit_students, hit_count, ratio, created_by, modified_by, is_deleted) " +
            "VALUES (#{questionTypeId}, #{kpUri}, #{gradeRange}, #{hitStudents}, #{hitCount}, #{ratio}, #{createdBy}, #{modifiedBy}, 0) " +
            "ON DUPLICATE KEY UPDATE grade_range = VALUES(grade_range), hit_students = VALUES(hit_students), " +
            "hit_count = VALUES(hit_count), ratio = VALUES(ratio), updated_at = NOW(), modified_by = VALUES(modified_by)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int upsert(QuestionTypeKpPo po);

    /** 按题型主表 ID 查全部分布桶（供解析先验）。 */
    @Select("SELECT * FROM t_kp_question_type_kp WHERE question_type_id = #{questionTypeId} AND is_deleted = false ORDER BY hit_count DESC")
    List<QuestionTypeKpPo> selectByQuestionTypeId(@Param("questionTypeId") Long questionTypeId);

    /** 查全部分布桶（供聚合变体合并预载题型 kp 签名）。 */
    @Select("SELECT * FROM t_kp_question_type_kp WHERE is_deleted = false")
    List<QuestionTypeKpPo> selectAll();
}
