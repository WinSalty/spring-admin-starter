package com.winsalty.quickstart.infra.redisson;

import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redisson 工具类测试。
 * 验证锁未获取和锁正常释放两个关键分支。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
class RedissonUtilTest {

    private static final String LOCK_KEY = "test:redisson:lock";
    private static final long WAIT_SECONDS = 1L;
    private static final long LEASE_SECONDS = 2L;

    /**
     * 未获取锁时返回 false，且不会执行传入任务。
     */
    @Test
    void tryLockShouldReturnFalseWhenLockNotAcquired() throws Exception {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(WAIT_SECONDS, LEASE_SECONDS, TimeUnit.SECONDS)).thenReturn(false);
        RedissonUtil redissonUtil = new RedissonUtil(redissonClient, properties());
        AtomicBoolean executed = new AtomicBoolean(false);

        boolean result = redissonUtil.tryLock(LOCK_KEY, WAIT_SECONDS, LEASE_SECONDS, () -> executed.set(true));

        assertFalse(result);
        assertFalse(executed.get());
        verify(lock, never()).unlock();
    }

    /**
     * 成功获取锁时执行任务，并在当前线程持锁时释放锁。
     */
    @Test
    void tryLockShouldExecuteTaskAndUnlockWhenAcquired() throws Exception {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(WAIT_SECONDS, LEASE_SECONDS, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        RedissonUtil redissonUtil = new RedissonUtil(redissonClient, properties());
        AtomicBoolean executed = new AtomicBoolean(false);

        boolean result = redissonUtil.tryLock(LOCK_KEY, WAIT_SECONDS, LEASE_SECONDS, () -> executed.set(true));

        assertTrue(result);
        assertTrue(executed.get());
        verify(lock).unlock();
    }

    private RedissonProperties properties() {
        RedissonProperties properties = new RedissonProperties();
        properties.setLockLogEnabled(false);
        return properties;
    }
}
