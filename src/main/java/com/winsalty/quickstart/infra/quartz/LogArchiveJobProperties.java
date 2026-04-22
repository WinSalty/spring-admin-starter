package com.winsalty.quickstart.infra.quartz;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 日志归档跑批配置。
 * 管理归档开关、保留天数、单批处理量和 Cron 表达式。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@ConfigurationProperties(prefix = "app.batch.log-archive")
public class LogArchiveJobProperties {

    private boolean enabled = true;
    private String cron = "0 0 2 * * ?";
    private int retentionDays = 30;
    private int batchSize = 1000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
