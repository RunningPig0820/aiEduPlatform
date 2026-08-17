package com.ai.edu.infrastructure.persistence.learning.mapper;

import com.ai.edu.infrastructure.persistence.learning.po.QuestionTypePo;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聚合题型库主表 Mapper（路由 ai_edu_learning 库）。
 *
 * <p>UPSERT 幂等：topic_label 唯一（uk_topic_label）。
 */
@Mapper
@DS("learning")
public interface QuestionTypeMapper extends BaseMapper<QuestionTypePo> {

    /**
     * 题型 UPSERT（topic_label 唯一，命中则更新状态/统计）。
     */
    @Insert("INSERT INTO t_kp_question_type (topic_label, status, definition, hit_students, hit_count, promoted_by, created_by, modified_by, is_deleted) " +
            "VALUES (#{topicLabel}, #{status}, #{definition}, #{hitStudents}, #{hitCount}, #{promotedBy}, #{createdBy}, #{modifiedBy}, 0) " +
            "ON DUPLICATE KEY UPDATE status = VALUES(status), definition = VALUES(definition), " +
            "hit_students = VALUES(hit_students), hit_count = VALUES(hit_count), " +
            "updated_at = NOW(), modified_by = VALUES(modified_by)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int upsert(QuestionTypePo po);

    /** 按题型 label 查条目。 */
    @Select("SELECT * FROM t_kp_question_type WHERE topic_label = #{topicLabel} AND is_deleted = false LIMIT 1")
    QuestionTypePo selectByTopicLabel(@Param("topicLabel") String topicLabel);

    /** 按 canonical 或别名命中题型（变体名 → canonical；canonical 优先）。 */
    @Select("SELECT qt.* FROM t_kp_question_type qt " +
            "LEFT JOIN t_kp_question_type_alias a ON a.question_type_id = qt.id AND a.is_deleted = false " +
            "WHERE qt.is_deleted = false AND (qt.topic_label = #{topicLabel} OR a.alias_label = #{topicLabel}) " +
            "ORDER BY (qt.topic_label = #{topicLabel}) DESC LIMIT 1")
    QuestionTypePo selectByTopicLabelOrAlias(@Param("topicLabel") String topicLabel);

    /** 按主键查条目（带逻辑删除过滤，避免与 BaseMapper.selectById 语义冲突）。 */
    @Select("SELECT * FROM t_kp_question_type WHERE id = #{id} AND is_deleted = false LIMIT 1")
    QuestionTypePo selectActiveById(@Param("id") Long id);

    /** 分页列题型（按 id 升序）。 */
    @Select("SELECT * FROM t_kp_question_type WHERE is_deleted = false ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<QuestionTypePo> selectPage(@Param("offset") int offset, @Param("limit") int limit);

    /** 题型总数。 */
    @Select("SELECT COUNT(*) FROM t_kp_question_type WHERE is_deleted = false")
    long count();

    /** 按命中数降序取常用题型名（题目理解参考词表，limit 上限）。 */
    @Select("SELECT topic_label FROM t_kp_question_type WHERE is_deleted = false ORDER BY hit_count DESC, hit_students DESC LIMIT #{limit}")
    List<String> selectTopTopicLabels(@Param("limit") int limit);

    /** 查全部题型（供聚合变体合并预载 kp 签名）。 */
    @Select("SELECT * FROM t_kp_question_type WHERE is_deleted = false ORDER BY id")
    List<QuestionTypePo> selectAll();
}
