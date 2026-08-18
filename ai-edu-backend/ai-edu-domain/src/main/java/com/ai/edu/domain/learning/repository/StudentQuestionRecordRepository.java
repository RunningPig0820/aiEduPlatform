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
}