package com.ai.edu.infrastructure.persistence.learning.mapper;

import com.ai.edu.infrastructure.persistence.learning.po.DerivedKpObsPo;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 个体派生观测 Mapper（路由 ai_edu_learning 库）。
 *
 * <p>UPSERT 幂等：student_id + topic_label + kp_uri 唯一（uk_student_topic_kp）。
 * kp_uri 为空（PENDING）不参与 UNIQUE 约束，去重经 selectPending + incrementOccurrence 处理。
 */
@Mapper
@DS("learning")
public interface DerivedKpObsMapper extends BaseMapper<DerivedKpObsPo> {

    /**
     * 观测 UPSERT（kp_uri 非空时经 UNIQUE 去重，命中则 occurrence_count +1，保留 first_seen_at）。
     */
    @Insert("INSERT INTO t_kp_derived_obs (student_id, topic_label, kp_uri, student_grade, confidence, source, status, occurrence_count, first_seen_at, created_by, modified_by, is_deleted) " +
            "VALUES (#{studentId}, #{topicLabel}, #{kpUri}, #{studentGrade}, #{confidence}, #{source}, #{status}, 1, NOW(), #{createdBy}, #{modifiedBy}, 0) " +
            "ON DUPLICATE KEY UPDATE occurrence_count = occurrence_count + 1, updated_at = NOW(), modified_by = VALUES(modified_by)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int upsert(DerivedKpObsPo po);

    /** 查 PENDING 观测（kp_uri 为空），供去重判断。 */
    @Select("SELECT * FROM t_kp_derived_obs WHERE student_id = #{studentId} AND topic_label = #{topicLabel} AND kp_uri IS NULL AND is_deleted = false LIMIT 1")
    DerivedKpObsPo selectPending(@Param("studentId") Long studentId, @Param("topicLabel") String topicLabel);

    /** PENDING 观测去重命中时递增 occurrence_count。 */
    @Update("UPDATE t_kp_derived_obs SET occurrence_count = occurrence_count + 1, updated_at = NOW() WHERE id = #{id}")
    int incrementOccurrence(@Param("id") Long id);

    /** 按学生查全部观测（供学生端疑似节点叠加）。 */
    @Select("SELECT * FROM t_kp_derived_obs WHERE student_id = #{studentId} AND is_deleted = false ORDER BY id")
    List<DerivedKpObsPo> selectByStudentId(@Param("studentId") Long studentId);

    /** 按题型 label 查观测（供聚合任务统计）。 */
    @Select("SELECT * FROM t_kp_derived_obs WHERE topic_label = #{topicLabel} AND is_deleted = false ORDER BY id")
    List<DerivedKpObsPo> selectByTopicLabel(@Param("topicLabel") String topicLabel);

    /** 查全部已解析观测（kp_uri 非空，供聚合任务扫描）。 */
    @Select("SELECT * FROM t_kp_derived_obs WHERE kp_uri IS NOT NULL AND is_deleted = false ORDER BY id")
    List<DerivedKpObsPo> selectResolved();

    /** 按状态查观测（供维护任务扫描）。 */
    @Select("SELECT * FROM t_kp_derived_obs WHERE status = #{status} AND is_deleted = false ORDER BY id")
    List<DerivedKpObsPo> selectByStatus(@Param("status") String status);

    /** 更新观测状态（重判转正 / 转人工）。 */
    @Update("UPDATE t_kp_derived_obs SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /** 统计同题型同知识点的去重学生数（第二信号共现检测）。 */
    @Select("SELECT COUNT(DISTINCT student_id) FROM t_kp_derived_obs WHERE topic_label = #{topicLabel} AND kp_uri = #{kpUri} AND is_deleted = false")
    int countDistinctStudentsByTopicKp(@Param("topicLabel") String topicLabel, @Param("kpUri") String kpUri);

    /** 人工确认挂起观测归属：更新 kp_uri + source=curated + status=RESOLVED。 */
    @Update("UPDATE t_kp_derived_obs SET kp_uri = #{kpUri}, source = 'curated', status = 'RESOLVED', updated_at = NOW() WHERE id = #{id}")
    int confirm(@Param("id") Long id, @Param("kpUri") String kpUri);
}
