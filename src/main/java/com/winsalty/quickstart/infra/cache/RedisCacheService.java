package com.winsalty.quickstart.infra.cache;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存服务。
 * 作为业务层访问 Redis 的薄封装，统一 key/value 序列化和过期时间单位。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Service
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 读取缓存对象，调用方负责按业务类型做强制转换。
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 写入带过期时间的缓存，timeoutSeconds 统一使用秒。
     */
    public void set(String key, Object value, long timeoutSeconds) {
        redisTemplate.opsForValue().set(key, value, timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * 原子自增，适用于缓存版本号、计数器或限流计数。
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
