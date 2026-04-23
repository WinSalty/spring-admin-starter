package com.winsalty.quickstart.common.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 核心依赖健康检查。
 * 显式探测数据库和 Redis 连通性，为运维提供更直接的应用就绪状态。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Component("coreDependencies")
public class CoreDependencyHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(CoreDependencyHealthIndicator.class);

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    public CoreDependencyHealthIndicator(DataSource dataSource,
                                         RedisConnectionFactory redisConnectionFactory) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        try (Connection connection = dataSource.getConnection()) {
            boolean databaseUp = connection.isValid(2);
            builder.withDetail("database", databaseUp ? "UP" : "DOWN");
            if (!databaseUp) {
                return Health.down().withDetail("database", "DOWN").build();
            }
        } catch (Exception exception) {
            log.error("database health check failed", exception);
            return Health.down(exception).withDetail("database", "DOWN").build();
        }
        try (RedisConnection redisConnection = redisConnectionFactory.getConnection()) {
            String pong = redisConnection.ping();
            if (!"PONG".equalsIgnoreCase(pong)) {
                return Health.down().withDetail("redis", "DOWN").build();
            }
            builder.withDetail("redis", "UP");
        } catch (Exception exception) {
            log.error("redis health check failed", exception);
            return Health.down(exception).withDetail("redis", "DOWN").build();
        }
        return builder.build();
    }
}
