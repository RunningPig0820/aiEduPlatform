package com.ai.edu.domain.learning.repository;

import com.ai.edu.domain.learning.model.entity.StudentQuestionRecord;

import java.util.List;

/**
 * 学生题目记录仓储接口（掌握度事实源：每道题一条记录）。
 *
 * <p>题目采集全量落库（AI 答疑 source=ai / 题库 bank 预留），一条作答一条记录（不做题目去重，
 * 同题做两次计两次训练量）。掌握表 {@code t_student_topic_mastery} 是聚合结果，由本表信号重算。
 */
public interface StudentQuestionRecordRepository {

    /** 保存一条题目记录（INSERT，主键经 useGeneratedKeys 回填）。 */
    StudentQuestionRecord save(StudentQuestionRecord record);

    /** 按学生查全部题目记录（掌握度追溯 / 题目列表页）。 */
    List<StudentQuestionRecord> findByStudentId(Long studentId);

    /** 按学生 + canonical 题型查题目记录（掌握度页「查看题目」，展示该题型全部证据题）。 */
    List<StudentQuestionRecord> findByStudentAndCanonical(Long studentId, String canonicalLabel);

    /** 批量聚集扫描：全部未归并（canonical_label IS NULL，PENDING）题型名（distinct，去空）。 */
    List<String> findPendingTopicLabels();

    /** 批量归并：某题型名下未归并记录 canonical_label 更新为目标值（幂等，仅更新 NULL）。返回更新行数。 */
    int updateCanonicalByTopic(String topicLabel, String canonicalLabel);

    /** 全量查题目记录（掌握表重算：内存按 student+canonical 分组累计平均）。 */
    List<StudentQuestionRecord> findAll();
}