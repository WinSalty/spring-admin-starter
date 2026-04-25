package com.winsalty.quickstart.infra.redisson;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 常用操作工具类。
 * 封装分布式锁、Bucket、Map 和原子计数器入口，统一锁等待/释放时间和排查日志。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
@Component
public class RedissonUtil {

    private static final Logger log = LoggerFactory.getLogger(RedissonUtil.class);
    private static final String LOCK_KEY_REQUIRED_MESSAGE = "lockKey must not be blank";
    private static final String BUCKET_KEY_REQUIRED_MESSAGE = "bucket key must not be blank";
    private static final String MAP_KEY_REQUIRED_MESSAGE = "map key must not be blank";
    private static final String ATOMIC_KEY_REQUIRED_MESSAGE = "atomic key must not be blank";
    private static final String TASK_REQUIRED_MESSAGE = "task must not be null";

    private final RedissonClient redissonClient;
    private final RedissonProperties properties;

    public RedissonUtil(RedissonClient redissonClient, RedissonProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    /**
     * 获取 Redisson 原生客户端，适用于业务需要使用队列、信号量等高级对象的场景。
     */
    public RedissonClient client() {
        return redissonClient;
    }

    /**
     * 使用默认等待时间和租约时间执行分布式锁保护的任务。
     */
    public boolean tryLock(String lockKey, Runnable task) {
        Assert.notNull(task, TASK_REQUIRED_MESSAGE);
        return tryLock(lockKey, properties.getLockWaitSeconds(), properties.getLockLeaseSeconds(), task);
    }

    /**
     * 使用指定等待时间和租约时间执行分布式锁保护的任务。
     */
    public boolean tryLock(String lockKey, long waitSeconds, long leaseSeconds, Runnable task) {
        Assert.notNull(task, TASK_REQUIRED_MESSAGE);
        Boolean result = tryLock(lockKey, waitSeconds, leaseSeconds, new Callable<Boolean>() {
            @Override
            public Boolean call() {
                task.run();
                return Boolean.TRUE;
            }
        });
        return Boolean.TRUE.equals(result);
    }

    /**
     * 使用默认等待时间和租约时间执行带返回值的分布式锁任务。
     */
    public <T> T tryLock(String lockKey, Callable<T> task) {
        return tryLock(lockKey, properties.getLockWaitSeconds(), properties.getLockLeaseSeconds(), task);
    }

    /**
     * 使用指定等待时间和租约时间执行带返回值的分布式锁任务。
     */
    public <T> T tryLock(String lockKey, long waitSeconds, long leaseSeconds, Callable<T> task) {
        Assert.hasText(lockKey, LOCK_KEY_REQUIRED_MESSAGE);
        Assert.notNull(task, TASK_REQUIRED_MESSAGE);
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            if (properties.isLockLogEnabled()) {
                log.info("redisson lock acquiring, lockKey={}, waitSeconds={}, leaseSeconds={}",
                        lockKey, waitSeconds, leaseSeconds);
            }
            locked = lock.tryLock(waitSeconds, leaseSeconds, TimeUnit.SECONDS);
            if (!locked) {
                log.info("redisson lock acquire skipped, lockKey={}, waitSeconds={}", lockKey, waitSeconds);
                return null;
            }
            if (properties.isLockLogEnabled()) {
                log.info("redisson lock acquired, lockKey={}", lockKey);
            }
            return task.call();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error("redisson lock interrupted, lockKey={}", lockKey, exception);
            throw new IllegalStateException("redisson lock interrupted", exception);
        } catch (RuntimeException exception) {
            log.error("redisson lock task failed, lockKey={}", lockKey, exception);
            throw exception;
        } catch (Exception exception) {
            log.error("redisson lock task failed, lockKey={}", lockKey, exception);
            throw new IllegalStateException("redisson lock task failed", exception);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                if (properties.isLockLogEnabled()) {
                    log.info("redisson lock released, lockKey={}", lockKey);
                }
            }
        }
    }

    /**
     * 获取 Redisson Bucket，适用于简单键值对象。
     */
    public <T> RBucket<T> bucket(String key) {
        Assert.hasText(key, BUCKET_KEY_REQUIRED_MESSAGE);
        return redissonClient.getBucket(key);
    }

    /**
     * 获取 Redisson Map，适用于分布式 Hash 结构。
     */
    public <K, V> RMap<K, V> map(String key) {
        Assert.hasText(key, MAP_KEY_REQUIRED_MESSAGE);
        return redissonClient.getMap(key);
    }

    /**
     * 获取 Redisson 原子长整型计数器，适用于全局序号和统计计数。
     */
    public RAtomicLong atomicLong(String key) {
        Assert.hasText(key, ATOMIC_KEY_REQUIRED_MESSAGE);
        return redissonClient.getAtomicLong(key);
    }
}
