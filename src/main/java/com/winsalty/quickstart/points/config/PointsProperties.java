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

    /** 是否开启对账能力。 */
    private boolean reconciliationEnabled = true;

    /** 默认冻结超时时间，单位秒。 */
    private long freezeDefaultExpireSeconds = 1800L;
}
