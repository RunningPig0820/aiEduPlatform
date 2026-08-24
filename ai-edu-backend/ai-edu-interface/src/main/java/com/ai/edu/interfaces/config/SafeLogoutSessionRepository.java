package com.ai.edu.interfaces.config;

import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

/**
 * 包装 Spring Session 的 {@link SessionRepository}，容忍"已失效会话被再次 save"。
 *
 * <p>背景：{@code RedisSessionRepository.save()} 会先检查 Redis key 是否仍在
 * （spring-session-data-redis 3.2.2 第 129 行），而 {@code session.invalidate()} 会立即删除
 * Redis key。于是登出请求结束时 {@code SessionRepositoryFilter.commitSession()} 无条件调用
 * {@code save()} 就会抛 {@link IllegalStateException}("Session was invalidated")，
 * 导致登出返回 500 且 /error 错误页也写不出来。
 * 本包装类仅吞掉这一个特定异常（Redis key 已被删 = 会话已失效，无需再落库），其余异常照常抛出。
 *
 * <p>注意：{@code RedisSessionRepository} 的内部会话类型 {@code RedisSession} 是包私有 final 类，
 * 外部包无法引用，故这里按 {@link SessionRepository}{@code <Session>} 实现并持有 raw delegate
 * （运行时传入的 session 一定是同一 repository 产出的 {@code RedisSession}，类型一致，安全）。
 */
@SuppressWarnings("rawtypes")
public class SafeLogoutSessionRepository implements SessionRepository<Session> {

    private final SessionRepository delegate;

    public SafeLogoutSessionRepository(SessionRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Session createSession() {
        return delegate.createSession();
    }

    @Override
    public void save(Session session) {
        try {
            delegate.save(session);
        } catch (IllegalStateException e) {
            if (!"Session was invalidated".equals(e.getMessage())) {
                throw e;
            }
            // 会话已失效（invalidate 已删除 Redis key），无需再 save，静默忽略
        }
    }

    @Override
    public Session findById(String id) {
        return delegate.findById(id);
    }

    @Override
    public void deleteById(String id) {
        delegate.deleteById(id);
    }
}
