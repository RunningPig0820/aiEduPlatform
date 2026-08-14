package com.ai.edu.infrastructure.persistence.learning.mapper;

import com.ai.edu.infrastructure.persistence.learning.po.QuestionTypePo;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
