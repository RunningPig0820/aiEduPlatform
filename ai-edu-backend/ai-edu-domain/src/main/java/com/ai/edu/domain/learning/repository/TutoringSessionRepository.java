package com.ai.edu.domain.learning.repository;

import com.ai.edu.domain.learning.model.entity.TutoringSession;

import java.util.List;
import java.util.Optional;

/**
 * 答疑会话仓储接口（实现于 Infrastructure 层，Mapper 经 {@code @DS("learning")} 路由 ai_edu_learning）。
 */
public interface TutoringSessionRepository {

    /**
     * 保存会话（含新会话创建与状态更新）。
     */
    TutoringSession save(TutoringSession session);

    /**
     * 按会话 ID 查找。
     */
    Optional<TutoringSession> findById(Long sessionId);

    /**
     * 按学生查活跃会话（断点恢复/频率统计）。
     */
    List<TutoringSession> findActiveByStudentId(Long studentId);

    /**
     * 按学生查全部会话（历史列表）：全状态（含已归档/终止），按 updated_at 倒序，不含软删。
     */
    List<TutoringSession> findListByStudentId(Long studentId);

    /**
     * 软删会话（is_deleted=1，全局逻辑删除；COS transcript/题目图片保留，可恢复）。
     */
    void softDelete(Long sessionId);

    /**
     * 回填 COS 对话归档 objectKey（首次实时写即调用）。
     */
    void updateTranscriptUrl(Long sessionId, String transcriptUrl);
}
