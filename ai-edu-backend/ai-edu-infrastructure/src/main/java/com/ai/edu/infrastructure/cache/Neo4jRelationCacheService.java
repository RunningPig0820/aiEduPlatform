package com.ai.edu.infrastructure.cache;

import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.ai.edu.domain.edukg.model.entity.relation.KgTextbookChapter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Neo4j 关联关系 Redis 缓存服务
 * TTL: 300s
 */
@Slf4j
@Service
public class Neo4jRelationCacheService {

    private static final String CACHE_PREFIX = "kg:neo4j:relation:";
    private static final long TTL_SECONDS = 300;

    @Resource
    private RedissonClient redissonClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取教材-章节关联缓存
     */
    public List<KgTextbookChapter> getTextbookChapterRelations(String textbookUri) {
        String key = CACHE_PREFIX + "textbook:" + textbookUri;
        return get(key, KgTextbookChapter.class);
    }

    /**
     * 设置教材-章节关联缓存
     */
    public void setTextbookChapterRelations(String textbookUri, List<KgTextbookChapter> relations) {
        String key = CACHE_PREFIX + "textbook:" + textbookUri;
        set(key, relations);
    }

    /**
     * 获取章节-小节关联缓存
     */
    public List<KgChapterSection> getChapterSectionRelations(String chapterUri) {
        String key = CACHE_PREFIX + "chapter:" + chapterUri;
        return get(key, KgChapterSection.class);
    }

    /**
     * 设置章节-小节关联缓存
     */
    public void setChapterSectionRelations(String chapterUri, List<KgChapterSection> relations) {
        String key = CACHE_PREFIX + "chapter:" + chapterUri;
        set(key, relations);
    }

    /**
     * 获取小节-知识点关联缓存
     */
    public List<KgSectionKP> getSectionKPRelations(String sectionUri) {
        String key = CACHE_PREFIX + "section:" + sectionUri;
        return get(key, KgSectionKP.class);
    }

    /**
     * 设置小节-知识点关联缓存
     */
    public void setSectionKPRelations(String sectionUri, List<KgSectionKP> relations) {
        String key = CACHE_PREFIX + "section:" + sectionUri;
        set(key, relations);
    }

    /**
     * 删除指定 URI 的关联缓存（按类型）
     */
    public void evictTextbook(String textbookUri) {
        String key = CACHE_PREFIX + "textbook:" + textbookUri;
        redissonClient.getBucket(key).delete();
        log.debug("Evicted cache: {}", key);
    }

    public void evictChapter(String chapterUri) {
        String key = CACHE_PREFIX + "chapter:" + chapterUri;
        redissonClient.getBucket(key).delete();
        log.debug("Evicted cache: {}", key);
    }

    public void evictSection(String sectionUri) {
        String key = CACHE_PREFIX + "section:" + sectionUri;
        redissonClient.getBucket(key).delete();
        log.debug("Evicted cache: {}", key);
    }

    private <T> List<T> get(String key, Class<T> elementType) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        String json = bucket.get();
        if (json == null) {
            return null;
        }
        try {
            var type = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, elementType);
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize cache key {}: {}", key, e.getMessage());
            return Collections.emptyList();
        }
    }

    private <T> void set(String key, List<T> value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            RBucket<String> bucket = redissonClient.getBucket(key);
            bucket.set(json, TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("Set cache: {} (TTL={}s)", key, TTL_SECONDS);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize cache key {}: {}", key, e.getMessage());
        }
    }
}
