package com.winsalty.quickstart.infra.json;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;

/**
 * Fastjson 序列化工具。
 * 统一封装项目内 JSON 转换入口，避免业务代码直接绑定多个 JSON 实现。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
public final class FastJsonUtils {

    private FastJsonUtils() {
    }

    /**
     * 将对象序列化为普通 JSON 字符串，适用于 HTTP 响应和日志摘要。
     */
    public static String toJsonString(Object value) {
        return JSON.toJSONString(value);
    }

    /**
     * 将对象按目标类型转换，适用于审计日志脱敏前把 DTO 转为 Map。
     */
    public static <T> T convert(Object value, Class<T> clazz) {
        if (value == null) {
            return null;
        }
        return JSON.parseObject(JSON.toJSONString(value), clazz);
    }

    /**
     * Redis 缓存序列化时写入类型信息，保障集合内 DTO 反序列化后仍可直接使用。
     */
    public static byte[] toTypedJsonBytes(Object value) {
        return JSON.toJSONBytes(value, JSONWriter.Feature.WriteClassName);
    }
}
