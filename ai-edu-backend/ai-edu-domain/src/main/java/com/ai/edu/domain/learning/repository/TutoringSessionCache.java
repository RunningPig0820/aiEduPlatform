package com.ai.edu.domain.learning.repository;

import com.ai.edu.domain.learning.model.contract.TutoringChatMessage;
import com.ai.edu.domain.learning.model.entity.TutoringSession;

import java.util.List;
import java.util.Optional;

/**
 * 答疑会话缓存端口（Redis，活跃期热存，TTL 24h）。
 *
 * <p>存储会话快照（生命周期 + 护栏计数器）+ 完整消息列表（供 decide 组装上下文与断点恢复）。
 * <b>不记录题目内容</b>（换题/当前题目判定在 Python）。另含会话创建频率计数。
 *
 * <p>实现位于 Infrastructure 层（{@code TutoringSessionCacheImpl}，见任务 8.5）。
 */
public interface TutoringSessionCache {

    /** 保存/更新会话快照（含计数器）。 */
    void saveSession(TutoringSession session);

    /** 按会话 ID 查快照（含缓存未命中）。 */
    Optional<TutoringSession> findSession(Long sessionId);

    /** 查该学生当前 ACTIVE 会话（断点恢复用，同一学生最多一个活跃会话）。 */
    Optional<TutoringSession> findActiveByStudentId(Long studentId);

    /** 追加一条消息到完整消息列表（列表尾）。 */
    void appendMessage(Long sessionId, TutoringChatMessage message);

    /** 列出该会话完整消息（按时间顺序）。 */
    List<TutoringChatMessage> listMessages(Long sessionId);

    /** 清空会话快照 + 消息列表（归档/终止后调用）。 */
    void clear(Long sessionId);

    /**
     * 会话创建频率限制：窗口内计数 +1，超过上限返回 false。
     *
     * @param studentId     学生 ID
     * @param windowMinutes 窗口分钟数（{@code SESSION_CREATE_WINDOW_MINUTES}=5）
     * @param limit         窗口内上限（{@code SESSION_CREATE_LIMIT}=3）
     * @return true=允许创建；false=过于频繁（超限）
     */
    boolean tryIncrementCreateCount(Long studentId, int windowMinutes, int limit);
}
