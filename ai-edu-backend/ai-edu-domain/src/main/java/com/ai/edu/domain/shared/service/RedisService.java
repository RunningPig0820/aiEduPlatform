package com.ai.edu.domain.shared.service;

import java.util.concurrent.TimeUnit;

/**
 * Redis 公共服务接口
 * 只提供通用的 Redis 操作，不包含业务逻辑
 *
 * @author AI Edu Platform
 */
public interface RedisService {

    /**
     * 设置值
     *
     * @param key   键
     * @param value 值
     */
    void set(String key, String value);

    /**
     * 设置值（带过期时间）
     *
     * @param key      键
     * @param value    值
     * @param timeout  过期时间
     * @param unit     时间单位
     */
    void set(String key, String value, long timeout, TimeUnit unit);

    /**
     * 获取值
     *
     * @param key 键
     * @return 值，不存在返回 null
     */
    String get(String key);

    /**
     * 删除键
     *
     * @param key 键
     * @return 是否删除成功
     */
    Boolean delete(String key);

    /**
     * 判断键是否存在
     *
     * @param key 键
     * @return 是否存在
     */
    Boolean hasKey(String key);

    /**
     * 设置过期时间
     *
     * @param key     键
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    Boolean expire(String key, long timeout, TimeUnit unit);

    /**
     * 自增
     *
     * @param key 键
     * @return 自增后的值
     */
    Long increment(String key);

    /**
     * 自增指定值
     *
     * @param key   键
     * @param delta 增量
     * @return 自增后的值
     */
    Long increment(String key, long delta);

    /**
     * 尝试获取分布式锁（SET NX EX 原子操作）
     *
     * @param key     锁键
     * @param value   锁值（用于释放时校验）
     * @param timeout 锁自动释放时间
     * @param unit    时间单位
     * @return 获取成功返回 true，失败返回 false
     */
    Boolean tryLock(String key, String value, long timeout, TimeUnit unit);

    /**
     * 释放分布式锁（Lua 脚本保证原子性：判断值相等再删除）
     *
     * @param key   锁键
     * @param value 锁值
     */
    void unlock(String key, String value);
}