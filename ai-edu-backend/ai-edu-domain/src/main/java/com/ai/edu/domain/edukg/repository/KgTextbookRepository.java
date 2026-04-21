package com.ai.edu.domain.edukg.repository;

import com.ai.edu.domain.edukg.model.entity.KgTextbook;

import java.util.List;
import java.util.Optional;

/**
 * 教材仓储接口
 */
public interface KgTextbookRepository {

    KgTextbook save(KgTextbook textbook);

    Optional<KgTextbook> findById(Long id);

    Optional<KgTextbook> findByUri(String uri);

    List<KgTextbook> findBySubject(String subject);

    List<KgTextbook> findBySubjectAndStage(String subject, String stage);

    List<KgTextbook> findAllActive();

    /**
     * 查询所有不重复的年级列表（用于下拉选择器）
     */
    List<String> findDistinctGrades();

    /**
     * 按学科查询不重复的年级列表（用于导航树）
     */
    List<String> findDistinctGradesBySubject(String subject);

    /**
     * 按版本+学科查询不重复的年级列表
     * 参数为 null 时表示不限制该维度
     */
    List<String> findDistinctGradesByEditionSubject(String edition, String subject);

    /**
     * 按版本+学科查询教材列表（返回 grade + uri）
     * 参数为 null 时表示不限制该维度
     */
    List<KgTextbook> findByEditionSubject(String edition, String subject);

    /**
     * 批量 UPSERT：按 URI 判断 insert 或 update
     * @return 插入的数量（不包括更新）
     */
    int upsert(List<KgTextbook> textbooks);

    void updateStatus(String uri, String status);

    /**
     * 按版本+学科+年级查询所有活跃教材
     */
    List<KgTextbook> findAllActiveByEditionSubjectGrade(String edition, String subject, String grade);
}
