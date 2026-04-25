package com.winsalty.quickstart.infra.redisson;

import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 基础配置。
 * 启用 Redisson 工具配置，并在应用启动后输出客户端初始化状态。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
@Configuration
@EnableConfigurationProperties(RedissonProperties.class)
public class RedissonConfig {

    private static final Logger log = LoggerFactory.getLogger(RedissonConfig.class);

    /**
     * Redisson 客户端启动日志，便于确认分布式锁能力已随应用初始化。
     */
    @Bean
    public ApplicationRunner redissonStartupLogger(RedissonClient redissonClient, RedissonProperties properties) {
        return args -> log.info("redisson client initialized, lockWaitSeconds={}, lockLeaseSeconds={}, lockLogEnabled={}",
                properties.getLockWaitSeconds(), properties.getLockLeaseSeconds(), properties.isLockLogEnabled());
    }
}
