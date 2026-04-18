package com.ai.edu.interface_;

import com.ai.edu.domain.shared.service.RedisService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 测试环境 Mock Redis 服务
 * 使用内存 Map 模拟 Redis 存储
 *
 * @author AI Edu Platform
 */
@Component
@Primary
public class MockRedisService implements RedisService {

    private final Map<String, String> store = new ConcurrentHashMap<>();
    private final Map<String, Long> expireTime = new ConcurrentHashMap<>();

    @Override
    public void set(String key, String value) {
        store.put(key, value);
    }

    @Override
    public void set(String key, String value, long timeout, TimeUnit unit) {
        store.put(key, value);
        expireTime.put(key, System.currentTimeMillis() + unit.toMillis(timeout));
    }

    @Override
    public String get(String key) {
        if (isExpired(key)) {
            store.remove(key);
            expireTime.remove(key);
            return null;
        }
        return store.get(key);
    }

    @Override
    public Boolean delete(String key) {
        expireTime.remove(key);
        return store.remove(key) != null;
    }

    @Override
    public Boolean hasKey(String key) {
        if (isExpired(key)) {
            store.remove(key);
            expireTime.remove(key);
            return false;
        }
        return store.containsKey(key);
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        if (store.containsKey(key)) {
            expireTime.put(key, System.currentTimeMillis() + unit.toMillis(timeout));
            return true;
        }
        return false;
    }

    @Override
    public Long increment(String key) {
        return increment(key, 1);
    }

    @Override
    public Long increment(String key, long delta) {
        String value = store.get(key);
        long newValue = (value == null ? 0 : Long.parseLong(value)) + delta;
        store.put(key, String.valueOf(newValue));
        return newValue;
    }

    private boolean isExpired(String key) {
        Long expire = expireTime.get(key);
        return expire != null && System.currentTimeMillis() > expire;
    }

    @Override
    public Boolean tryLock(String key, String value, long timeout, TimeUnit unit) {
        if (isExpired(key)) {
            store.remove(key);
            expireTime.remove(key);
        }
        if (store.containsKey(key)) {
            return false;
        }
        store.put(key, value);
        expireTime.put(key, System.currentTimeMillis() + unit.toMillis(timeout));
        return true;
    }

    @Override
    public void unlock(String key, String value) {
        String current = store.get(key);
        if (value.equals(current)) {
            store.remove(key);
            expireTime.remove(key);
        }
    }
}