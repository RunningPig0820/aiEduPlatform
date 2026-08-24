package com.ai.edu.interfaces.config;

import org.junit.jupiter.api.Test;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SafeLogoutSessionRepository} 单元测试：
 * 只吞"已失效会话被再次 save"这一个特定异常，其余异常照抛，其余方法透传。
 */
class SafeLogoutSessionRepositoryTest {

    @SuppressWarnings({"rawtypes", "unchecked"})
    private final SessionRepository delegate = mock(SessionRepository.class);

    @SuppressWarnings("rawtypes")
    private final SafeLogoutSessionRepository repository = new SafeLogoutSessionRepository(delegate);

    private final Session session = mock(Session.class);

    @Test
    void save_swallowsInvalidatedSessionException() {
        doThrow(new IllegalStateException("Session was invalidated")).when(delegate).save(session);
        assertDoesNotThrow(() -> repository.save(session));
    }

    @Test
    void save_rethrowsOtherIllegalStateException() {
        doThrow(new IllegalStateException("some other error")).when(delegate).save(session);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> repository.save(session));
        assertEquals("some other error", e.getMessage());
    }

    @Test
    void save_delegatesNormally() {
        repository.save(session);
        verify(delegate).save(session);
    }

    @Test
    void createSession_findById_deleteById_delegateThrough() {
        when(delegate.createSession()).thenReturn(session);
        when(delegate.findById("abc")).thenReturn(session);

        assertSame(session, repository.createSession());
        assertSame(session, repository.findById("abc"));

        repository.deleteById("abc");
        verify(delegate).deleteById("abc");
    }
}
