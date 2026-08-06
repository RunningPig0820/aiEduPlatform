package com.ai.edu.infrastructure.cache;

import com.ai.edu.domain.learning.model.contract.TutoringChatMessage;
import com.ai.edu.domain.learning.model.entity.TutoringSession;
import com.ai.edu.domain.learning.model.valueobject.EndReason;
import com.ai.edu.domain.learning.model.valueobject.TutoringState;
import com.ai.edu.domain.shared.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 答疑会话缓存（Redis）单元测试——全 Mock RedissonClient + RBucket + RedisService。
 */
class TutoringSessionCacheImplTest {

    private static final long STUDENT_ID = 501L;
    private static final long SESSION_ID = 1001L;
    private static final long TTL_SECONDS = 24 * 60 * 60L;

    private TutoringSessionCacheImpl cache;
    private RedissonClient redissonClient;
    private RedisService redisService;
    @SuppressWarnings("rawtypes")
    private RBucket mockBucket;

    @BeforeEach
    void setUp() {
        redissonClient = Mockito.mock(RedissonClient.class);
        redisService = Mockito.mock(RedisService.class);
        mockBucket = Mockito.mock(RBucket.class);

        cache = new TutoringSessionCacheImpl();
        setField(cache, "redissonClient", redissonClient);
        setField(cache, "redisService", redisService);

        when(redissonClient.getBucket(anyString())).thenReturn(mockBucket);
    }

    // ==================== saveSession / findSession ====================

    @Test
    @DisplayName("saveSession ACTIVE：写会话快照(带TTL) + 维护活跃索引")
    void saveSession_active_keepsIndex() {
        TutoringSession session = TutoringSession.start(STUDENT_ID, "math");
        session.setId(SESSION_ID);

        cache.saveSession(session);

        verify(redissonClient).getBucket("learning:tutoring:session:" + SESSION_ID);
        verify(mockBucket).set(anyString(), eq(TTL_SECONDS), eq(TimeUnit.SECONDS));
        verify(redisService).set(eq("learning:tutoring:active:" + STUDENT_ID),
                eq(String.valueOf(SESSION_ID)), eq(TTL_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("saveSession 非 ACTIVE：写快照但清活跃索引")
    void saveSession_ended_clearsIndex() {
        TutoringSession session = TutoringSession.start(STUDENT_ID, "math");
        session.setId(SESSION_ID);
        session.complete(EndReason.COMPLETED);

        cache.saveSession(session);

        verify(mockBucket).set(anyString(), eq(TTL_SECONDS), eq(TimeUnit.SECONDS));
        verify(redisService).delete("learning:tutoring:active:" + STUDENT_ID);
    }

    @Test
    @DisplayName("findSession：会话快照 JSON 往返，状态/计数完整恢复")
    void findSession_roundTrip() throws Exception {
        TutoringSession session = TutoringSession.start(STUDENT_ID, "math");
        session.setId(SESSION_ID);
        session.recordRound();
        session.requestAnswer();
        String json = new ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setVisibility(com.fasterxml.jackson.annotation.PropertyAccessor.FIELD,
                        com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
                .writeValueAsString(session);
        when(mockBucket.get()).thenReturn(json);

        Optional<TutoringSession> found = cache.findSession(SESSION_ID);

        assertTrue(found.isPresent());
        assertEquals(SESSION_ID, found.get().getId());
        assertEquals(STUDENT_ID, found.get().getStudentId());
        assertEquals(TutoringState.ACTIVE, found.get().getStatus());
        assertEquals(1, found.get().getRoundCount());
        assertEquals(1, found.get().getAnswerRequestCount());
    }

    @Test
    @DisplayName("findSession 未命中返回 empty")
    void findSession_miss() {
        when(mockBucket.get()).thenReturn(null);
        assertTrue(cache.findSession(SESSION_ID).isEmpty());
    }

    // ==================== findActiveByStudentId ====================

    @Test
    @DisplayName("findActiveByStudentId：读索引 → 取会话快照")
    void findActiveByStudentId_hit() {
        when(redisService.get("learning:tutoring:active:" + STUDENT_ID)).thenReturn(String.valueOf(SESSION_ID));
        TutoringSession session = TutoringSession.start(STUDENT_ID, "math");
        session.setId(SESSION_ID);
        when(mockBucket.get()).thenReturn(jsonOf(session));

        Optional<TutoringSession> found = cache.findActiveByStudentId(STUDENT_ID);

        assertTrue(found.isPresent());
        assertEquals(SESSION_ID, found.get().getId());
    }

    @Test
    @DisplayName("findActiveByStudentId 索引为空返回 empty")
    void findActiveByStudentId_miss() {
        when(redisService.get("learning:tutoring:active:" + STUDENT_ID)).thenReturn(null);
        assertTrue(cache.findActiveByStudentId(STUDENT_ID).isEmpty());
    }

    // ==================== 消息列表 ====================

    @Test
    @DisplayName("appendMessage：读旧列表追加并整写（带TTL）")
    void appendMessage_appendsAndWrites() {
        TutoringChatMessage first = TutoringChatMessage.user("鸡兔同笼");
        when(mockBucket.get()).thenReturn(jsonOf(List.of(first)));

        cache.appendMessage(SESSION_ID, TutoringChatMessage.ai("先找已知条件"));

        verify(redissonClient, atLeastOnce()).getBucket("learning:tutoring:messages:" + SESSION_ID);
        verify(mockBucket).set(contains("\"先找已知条件\""), eq(TTL_SECONDS), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("listMessages：读完整消息列表")
    void listMessages_returnsList() {
        when(mockBucket.get()).thenReturn(jsonOf(List.of(
                TutoringChatMessage.user("设x"), TutoringChatMessage.ai("继续"))));

        List<TutoringChatMessage> messages = cache.listMessages(SESSION_ID);

        assertEquals(2, messages.size());
        assertEquals("设x", messages.get(0).getContent());
    }

    @Test
    @DisplayName("listMessages 未命中返回空列表")
    void listMessages_miss() {
        when(mockBucket.get()).thenReturn(null);
        assertTrue(cache.listMessages(SESSION_ID).isEmpty());
    }

    // ==================== clear ====================

    @Test
    @DisplayName("clear：清快照+消息+活跃索引")
    void clear_removesAll() {
        TutoringSession session = TutoringSession.start(STUDENT_ID, "math");
        session.setId(SESSION_ID);
        when(mockBucket.get()).thenReturn(jsonOf(session));

        cache.clear(SESSION_ID);

        verify(redisService).delete("learning:tutoring:active:" + STUDENT_ID);
        verify(redissonClient, atLeastOnce()).getBucket("learning:tutoring:session:" + SESSION_ID);
        verify(redissonClient).getBucket("learning:tutoring:messages:" + SESSION_ID);
    }

    // ==================== 频率计数 ====================

    @Test
    @DisplayName("tryIncrementCreateCount：首次自增设窗口过期，未超限放行")
    void tryIncrementCreateCount_allowed() {
        when(redisService.increment("learning:tutoring:create:" + STUDENT_ID)).thenReturn(2L);

        boolean allowed = cache.tryIncrementCreateCount(STUDENT_ID, 5, 3);

        assertTrue(allowed);
        verify(redisService, never()).expire(anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("tryIncrementCreateCount：首次调用设置窗口过期")
    void tryIncrementCreateCount_firstSetsExpire() {
        when(redisService.increment("learning:tutoring:create:" + STUDENT_ID)).thenReturn(1L);

        cache.tryIncrementCreateCount(STUDENT_ID, 5, 3);

        verify(redisService).expire("learning:tutoring:create:" + STUDENT_ID, 5L, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("tryIncrementCreateCount：超过上限拒绝")
    void tryIncrementCreateCount_rejected() {
        when(redisService.increment("learning:tutoring:create:" + STUDENT_ID)).thenReturn(4L);

        boolean allowed = cache.tryIncrementCreateCount(STUDENT_ID, 5, 3);

        assertFalse(allowed);
    }

    // ==================== helpers ====================

    private String jsonOf(Object value) {
        try {
            return new ObjectMapper()
                    .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                    .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .setVisibility(com.fasterxml.jackson.annotation.PropertyAccessor.FIELD,
                            com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
                    .writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = TutoringSessionCacheImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
