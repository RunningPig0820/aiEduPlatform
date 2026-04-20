package com.ai.edu.domain.edukg.repository;

import com.ai.edu.domain.edukg.model.entity.KgSyncRecord;

import java.util.List;
import java.util.Optional;

/**
 * 同步记录仓储接口
 */
public interface KgSyncRecordRepository {

    /**
     * 保存同步记录（新增或更新）
     */
    KgSyncRecord save(KgSyncRecord record);

    /**
     * 查询最近的同步记录
     */
    List<KgSyncRecord> findRecent(int limit);

    /**
     * 根据 ID 查询同步记录
     */
    Optional<KgSyncRecord> findById(Long id);

    /**
     * 按同步范围查询最新运行中的同步记录（用于检测过期任务）
     */
    Optional<KgSyncRecord> findLatestRunningByScope(
            String edition, String subject, String stage, String grade);
}
