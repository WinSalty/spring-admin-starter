package com.winsalty.quickstart.infra.cache;

import com.winsalty.quickstart.infra.json.FastJsonUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
     * 按指定类型读取缓存对象，适用于调用方需要避免强制转换的场景。
     */
    public <T> T get(String key, Class<T> clazz) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return FastJsonUtils.convert(value, clazz);
    }

    /**
     * 写入不带过期时间的缓存，只允许用于长期配置类数据。
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 写入带过期时间的缓存，timeoutSeconds 统一使用秒。
     */
    public void set(String key, Object value, long timeoutSeconds) {
        // 所有业务缓存都必须显式给 TTL，避免验证码、会话这类短期数据永久滞留。
        redisTemplate.opsForValue().set(key, value, timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * 缓存压缩后的 Java 对象。大对象先 JSON 序列化再 GZIP，降低 Redis 内存占用。
     */
    public void setCompressedObject(String key, Object value, long timeoutSeconds) {
        String compressedValue = compress(FastJsonUtils.toJsonString(value));
        set(key, compressedValue, timeoutSeconds);
    }

    /**
     * 读取压缩缓存对象并反序列化为指定 Java 类型。
     */
    public <T> T getCompressedObject(String key, Class<T> clazz) {
        Object cached = get(key);
        if (!(cached instanceof String)) {
            return null;
        }
        String json = decompress((String) cached);
        return com.alibaba.fastjson2.JSON.parseObject(json, clazz);
    }

    /**
     * 仅当 key 不存在时写入，适用于分布式锁和幂等占位。
     */
    public Boolean setIfAbsent(String key, Object value, long timeoutSeconds) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * 原子自增，适用于缓存版本号、计数器或限流计数。
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 按指定步长原子自增。
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 为已有 key 设置过期时间，常用于计数器首次创建后的限流窗口绑定。
     */
    public void expire(String key, long timeoutSeconds) {
        redisTemplate.expire(key, timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * 查询 key 剩余过期时间，单位秒。
     */
    public Long ttl(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断 key 是否存在。
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public void delete(String key) {
        // 删除操作用于一次性验证码消费和 refresh session 失效，调用方不需要关心 key 是否存在。
        redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存 key。
     */
    public void delete(Collection<String> keys) {
        redisTemplate.delete(keys);
    }

    /**
     * 写入 Hash 字段。
     */
    public void hashPut(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 读取 Hash 字段。
     */
    public Object hashGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 删除 Hash 字段。
     */
    public Long hashDelete(String key, Object... hashKeys) {
        return redisTemplate.opsForHash().delete(key, hashKeys);
    }

    /**
     * 向 List 右侧追加元素。
     */
    public Long rightPush(String key, Object value) {
        return redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 从 List 左侧弹出元素。
     */
    public Object leftPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 按范围读取 List。
     */
    public List<Object> listRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 向 Set 添加元素。
     */
    public Long setAdd(String key, Object... values) {
        return redisTemplate.opsForSet().add(key, values);
    }

    /**
     * 读取 Set 全部成员。
     */
    public Set<Object> setMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 从 Set 移除元素。
     */
    public Long setRemove(String key, Object... values) {
        return redisTemplate.opsForSet().remove(key, values);
    }

    private String compress(String value) {
        if (value == null) {
            return "";
        }
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
            gzipOutputStream.write(value.getBytes(StandardCharsets.UTF_8));
            gzipOutputStream.close();
            return java.util.Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("redis cache compress failed", exception);
        }
    }

    private String decompress(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(value);
            GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(bytes));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[512];
            int length;
            while ((length = gzipInputStream.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            return new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("redis cache decompress failed", exception);
        }
    }
}
