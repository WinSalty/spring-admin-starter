package com.winsalty.quickstart.infra.outbox;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 本地事务事件任务配置。
 * 管理 outbox 扫描开关和 Cron 表达式。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.outbox")
public class TransactionOutboxProperties {

    private static final String DEFAULT_CRON = "0 */1 * * * ?";

    /** 是否启用 outbox 任务。 */
    private boolean enabled = true;

    /** outbox 扫描 Cron 表达式。 */
    private String cron = DEFAULT_CRON;
}
