package com.winsalty.quickstart.points.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 积分模块配置。
 * 通过 app.points 前缀接收环境配置，默认值保证开发环境可启动。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.points")
public class PointsProperties {

    private static final String DEFAULT_RECONCILIATION_CRON = "0 10 2 * * ?";
    private static final String DEFAULT_FREEZE_COMPENSATION_CRON = "0 */10 * * * ?";

    /** 是否开启对账能力。 */
    private boolean reconciliationEnabled = true;

    /** 积分对账任务 Cron 表达式。 */
    private String reconciliationCron = DEFAULT_RECONCILIATION_CRON;

    /** 默认冻结超时时间，单位秒。 */
    private long freezeDefaultExpireSeconds = 1800L;

    /** 是否开启冻结单过期补偿。 */
    private boolean freezeCompensationEnabled = true;

    /** 冻结单过期补偿 Cron 表达式。 */
    private String freezeCompensationCron = DEFAULT_FREEZE_COMPENSATION_CRON;

    /** 冻结单补偿单批处理量。 */
    private int freezeCompensationBatchSize = 100;
}
