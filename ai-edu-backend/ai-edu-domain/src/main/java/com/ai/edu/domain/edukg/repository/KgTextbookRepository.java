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

    List<KgTextbook> findBySubjectAndPhase(String subject, String phase);

    List<KgTextbook> findAllActive();

    /**
     * 查询所有不重复的年级列表（用于下拉选择器）
     */
    List<String> findDistinctGrades();

    /**
     * 按学科查询不重复的年级列表（用于导航树）
     */
    List<String> findDistinctGradesBySubject(String subject);

    void updateStatus(String uri, String status);
}
