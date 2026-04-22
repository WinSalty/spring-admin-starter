package com.winsalty.quickstart.infra.cache;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.winsalty.quickstart.infra.json.FastJsonUtils;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.StandardCharsets;

/**
 * Fastjson Redis 序列化器。
 * 使用 JSON 保存缓存值，并保留业务 DTO 类型信息，便于列表缓存直接反序列化使用。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
public class FastJsonRedisSerializer implements RedisSerializer<Object> {

    private static final JSONReader.AutoTypeBeforeHandler AUTO_TYPE_FILTER = JSONReader.autoTypeFilter(
            "com.winsalty.quickstart.",
            "java.util.",
            "java.lang."
    );

    @Override
    public byte[] serialize(Object value) throws SerializationException {
        if (value == null) {
            return new byte[0];
        }
        return FastJsonUtils.toTypedJsonBytes(value);
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return JSON.parseObject(bytes, Object.class, AUTO_TYPE_FILTER, JSONReader.Feature.SupportAutoType);
        } catch (RuntimeException exception) {
            String value = new String(bytes, StandardCharsets.UTF_8);
            throw new SerializationException("Fastjson redis deserialize failed, value=" + value, exception);
        }
    }
}
