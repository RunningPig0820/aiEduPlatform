package com.ai.edu.infrastructure.persistence.learning.mapper;

import com.ai.edu.infrastructure.persistence.learning.po.QuestionTypeAliasPo;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 题型库变体别名 Mapper（路由 ai_edu_learning 库）。
 *
 * <p>UPSERT 幂等：alias_label 唯一（uk_alias_label）。
 */
@Mapper
@DS("learning")
public interface QuestionTypeAliasMapper extends BaseMapper<QuestionTypeAliasPo> {

    /**
     * 别名 UPSERT（alias_label 唯一，命中则更新关联题型）。
     */
    @Insert("INSERT INTO t_kp_question_type_alias (alias_label, question_type_id, created_by, modified_by, is_deleted) " +
            "VALUES (#{aliasLabel}, #{questionTypeId}, #{createdBy}, #{modifiedBy}, 0) " +
            "ON DUPLICATE KEY UPDATE question_type_id = VALUES(question_type_id), " +
            "updated_at = NOW(), modified_by = VALUES(modified_by)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int upsert(QuestionTypeAliasPo po);

    /** 按变体题型名查别名。 */
    @Select("SELECT * FROM t_kp_question_type_alias WHERE alias_label = #{aliasLabel} AND is_deleted = false LIMIT 1")
    QuestionTypeAliasPo selectByAliasLabel(@Param("aliasLabel") String aliasLabel);

    /** 按 canonical 题型查全部别名（供聚合变体合并 union 重建）。 */
    @Select("SELECT * FROM t_kp_question_type_alias WHERE question_type_id = #{questionTypeId} AND is_deleted = false")
    List<QuestionTypeAliasPo> selectByQuestionTypeId(@Param("questionTypeId") Long questionTypeId);
}
