package com.winsalty.quickstart.infra.redisson;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redisson 工具默认配置。
 * 管理分布式锁默认等待时间、自动释放时间和日志开关。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
@ConfigurationProperties(prefix = "app.redisson")
public class RedissonProperties {

    private static final long DEFAULT_LOCK_WAIT_SECONDS = 3L;
    private static final long DEFAULT_LOCK_LEASE_SECONDS = 30L;
    private static final boolean DEFAULT_LOCK_LOG_ENABLED = true;

    private long lockWaitSeconds = DEFAULT_LOCK_WAIT_SECONDS;
    private long lockLeaseSeconds = DEFAULT_LOCK_LEASE_SECONDS;
    private boolean lockLogEnabled = DEFAULT_LOCK_LOG_ENABLED;

    public long getLockWaitSeconds() {
        return lockWaitSeconds;
    }

    public void setLockWaitSeconds(long lockWaitSeconds) {
        this.lockWaitSeconds = lockWaitSeconds;
    }

    public long getLockLeaseSeconds() {
        return lockLeaseSeconds;
    }

    public void setLockLeaseSeconds(long lockLeaseSeconds) {
        this.lockLeaseSeconds = lockLeaseSeconds;
    }

    public boolean isLockLogEnabled() {
        return lockLogEnabled;
    }

    public void setLockLogEnabled(boolean lockLogEnabled) {
        this.lockLogEnabled = lockLogEnabled;
    }
}
