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

    void updateStatus(String uri, String status);
}
