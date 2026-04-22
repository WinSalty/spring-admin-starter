package com.winsalty.quickstart.infra.config;

import com.winsalty.quickstart.infra.cache.FastJsonRedisSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置。
 * 使用 String key + Fastjson value，便于从 redis-cli 直接观察缓存内容。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Configuration
public class RedisConfig {

    /**
     * 统一 RedisTemplate 序列化策略，避免默认 JDK 序列化导致 key/value 难以排查。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        FastJsonRedisSerializer jsonRedisSerializer = new FastJsonRedisSerializer();
        redisTemplate.setConnectionFactory(connectionFactory);
        // key 使用纯字符串，避免 redis-cli 里出现 JDK 序列化前缀，便于人工排查 session/验证码。
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        // value 用 Fastjson 保留类型信息，业务层取出后仍可按 String、Map 等常见结构使用。
        redisTemplate.setValueSerializer(jsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jsonRedisSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
