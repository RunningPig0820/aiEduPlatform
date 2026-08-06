package com.ai.edu.infrastructure.cache;

import com.ai.edu.domain.learning.model.contract.TutoringChatMessage;
import com.ai.edu.domain.learning.model.entity.TutoringSession;
import com.ai.edu.domain.learning.model.valueobject.TutoringState;
import com.ai.edu.domain.learning.repository.TutoringSessionCache;
import com.ai.edu.domain.shared.service.RedisService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 答疑会话缓存实现（Redis，活跃期热存，TTL 24h）。
 *
 * <p>存储：会话快照（生命周期 + 护栏计数器）+ 完整消息列表（供 decide 组装上下文与断点恢复）
 * + 该学生当前 ACTIVE 会话索引 + 创建频率计数。<b>不记录题目内容</b>。
 *
 * <p>序列化：Jackson JSON String Bucket（照 {@link Neo4jRelationCacheService} 模式）；
 * 会话快照用 FIELD 可见性序列化领域聚合根（私有字段 + 私有无参构造可往返）。
 */
@Slf4j
@Service
public class TutoringSessionCacheImpl implements TutoringSessionCache {

    private static final String SESSION_PREFIX = "learning:tutoring:session:";
    private static final String MESSAGE_PREFIX = "learning:tutoring:messages:";
    private static final String ACTIVE_PREFIX = "learning:tutoring:active:";
    private static final String CREATE_COUNT_PREFIX = "learning:tutoring:create:";

    /** TTL 24h（活跃会话热存，断点恢复窗口）。 */
    private static final long TTL_SECONDS = 24 * 60 * 60L;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RedisService redisService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    @Override
    public void saveSession(TutoringSession session) {
        if (session == null || session.getId() == null) {
            log.warn("saveSession 跳过：会话为 null 或未生成主键");
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(session);
            RBucket<String> bucket = redissonClient.getBucket(SESSION_PREFIX + session.getId());
            bucket.set(json, TTL_SECONDS, TimeUnit.SECONDS);
            maintainActiveIndex(session);
        } catch (JsonProcessingException e) {
            log.error("会话快照序列化失败: sessionId={}", session.getId(), e);
        }
    }

    @Override
    public Optional<TutoringSession> findSession(Long sessionId) {
        RBucket<String> bucket = redissonClient.getBucket(SESSION_PREFIX + sessionId);
        String json = bucket.get();
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(objectMapper.readValue(json, TutoringSession.class));
        } catch (JsonProcessingException e) {
            log.warn("会话快照反序列化失败: sessionId={}: {}", sessionId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<TutoringSession> findActiveByStudentId(Long studentId) {
        String sessionIdStr = redisService.get(ACTIVE_PREFIX + studentId);
        if (sessionIdStr == null || sessionIdStr.isBlank()) {
            return Optional.empty();
        }
        try {
            Long sessionId = Long.valueOf(sessionIdStr);
            return findSession(sessionId);
        } catch (NumberFormatException e) {
            log.warn("活跃会话索引非法: studentId={}, value={}", studentId, sessionIdStr);
            return Optional.empty();
        }
    }

    @Override
    public void appendMessage(Long sessionId, TutoringChatMessage message) {
        List<TutoringChatMessage> messages = listMessages(sessionId);
        messages.add(message);
        try {
            String json = objectMapper.writeValueAsString(messages);
            RBucket<String> bucket = redissonClient.getBucket(MESSAGE_PREFIX + sessionId);
            bucket.set(json, TTL_SECONDS, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("消息列表序列化失败: sessionId={}", sessionId, e);
        }
    }

    @Override
    public List<TutoringChatMessage> listMessages(Long sessionId) {
        RBucket<String> bucket = redissonClient.getBucket(MESSAGE_PREFIX + sessionId);
        String json = bucket.get();
        if (json == null) {
            return new java.util.ArrayList<>();
        }
        try {
            var type = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, TutoringChatMessage.class);
            return new java.util.ArrayList<>(objectMapper.readValue(json, type));
        } catch (JsonProcessingException e) {
            log.warn("消息列表反序列化失败: sessionId={}: {}", sessionId, e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    @Override
    public void clear(Long sessionId) {
        findSession(sessionId).ifPresent(session ->
                redisService.delete(ACTIVE_PREFIX + session.getStudentId()));
        redissonClient.getBucket(SESSION_PREFIX + sessionId).delete();
        redissonClient.getBucket(MESSAGE_PREFIX + sessionId).delete();
    }

    @Override
    public boolean tryIncrementCreateCount(Long studentId, int windowMinutes, int limit) {
        String key = CREATE_COUNT_PREFIX + studentId;
        Long count = redisService.increment(key);
        if (count != null && count == 1L) {
            redisService.expire(key, windowMinutes, TimeUnit.MINUTES);
        }
        return count != null && count <= limit;
    }

    /** 维护该学生"当前 ACTIVE 会话"索引（同一学生最多一个活跃会话，断点恢复用）。 */
    private void maintainActiveIndex(TutoringSession session) {
        String indexKey = ACTIVE_PREFIX + session.getStudentId();
        if (session.isActive()) {
            redisService.set(indexKey, String.valueOf(session.getId()), TTL_SECONDS, TimeUnit.SECONDS);
        } else {
            redisService.delete(indexKey);
        }
    }
}
