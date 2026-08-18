package com.ai.edu.infrastructure.persistence.learning.mapper;

import com.ai.edu.infrastructure.persistence.learning.po.StudentQuestionRecordPo;
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
 * 学生题目记录 Mapper（路由 ai_edu_learning 库）。
 *
 * <p>掌握度事实源：一条作答一条记录（INSERT，不做去重）。canonical_label 可空（PENDING），
 * 派生查询 student_id 索引 + student_id+canonical_label 联合索引支撑。
 */
@Mapper
@DS("learning")
public interface StudentQuestionRecordMapper extends BaseMapper<StudentQuestionRecordPo> {

    /**
     * 保存题目记录（INSERT，useGeneratedKeys 回填主键；不做题目去重，一次作答一条）。
     */
    @Insert("INSERT INTO t_student_question_record (student_id, content, source, topic_label, canonical_label, score, hint_count, answer_request_count, session_id, created_at, updated_at, created_by, modified_by, is_deleted) " +
            "VALUES (#{studentId}, #{content}, #{source}, #{topicLabel}, #{canonicalLabel}, #{score}, #{hintCount}, #{answerRequestCount}, #{sessionId}, #{createdAt}, #{updatedAt}, #{createdBy}, #{modifiedBy}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(StudentQuestionRecordPo po);

    /** 按学生查全部题目记录（掌握度追溯 / 题目列表页，时间正序）。 */
    @Select("SELECT * FROM t_student_question_record WHERE student_id = #{studentId} AND is_deleted = false ORDER BY id")
    List<StudentQuestionRecordPo> selectByStudentId(@Param("studentId") Long studentId);

    /** 按学生 + canonical 题型查题目记录（掌握度页「查看题目」，展示该题型证据题）。 */
    @Select("SELECT * FROM t_student_question_record WHERE student_id = #{studentId} AND canonical_label = #{canonicalLabel} AND is_deleted = false ORDER BY id")
    List<StudentQuestionRecordPo> selectByStudentAndCanonical(@Param("studentId") Long studentId, @Param("canonicalLabel") String canonicalLabel);

    /** 批量聚集扫描：未归并（canonical_label IS NULL，PENDING）题型名（distinct，去空）。 */
    @Select("SELECT DISTINCT topic_label FROM t_student_question_record WHERE canonical_label IS NULL AND is_deleted = false AND topic_label IS NOT NULL AND topic_label <> ''")
    List<String> selectPendingTopicLabels();

    /** 按学生查未归并题型名（getMastery PENDING 项来源，distinct 去空）。 */
    @Select("SELECT DISTINCT topic_label FROM t_student_question_record WHERE student_id = #{studentId} AND canonical_label IS NULL AND is_deleted = false AND topic_label IS NOT NULL AND topic_label <> ''")
    List<String> selectPendingTopicLabelsByStudent(@Param("studentId") Long studentId);

    /** 批量归并：某题型名下未归并记录 canonical_label 更新为目标值（幂等，仅更新 NULL）。 */
    @Update("UPDATE t_student_question_record SET canonical_label = #{canonicalLabel}, updated_at = NOW() WHERE topic_label = #{topicLabel} AND canonical_label IS NULL AND is_deleted = false")
    int updateCanonicalByTopic(@Param("topicLabel") String topicLabel, @Param("canonicalLabel") String canonicalLabel);

    /** 全量查题目记录（掌握表重算：内存按 student+canonical 分组累计平均）。 */
    @Select("SELECT * FROM t_student_question_record WHERE is_deleted = false")
    List<StudentQuestionRecordPo> selectAll();
}