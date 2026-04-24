package com.winsalty.quickstart.cdk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CDK 模块配置。
 * 通过 app.cdk 前缀接收环境配置，pepper 必须由生产环境显式注入。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.cdk")
public class CdkProperties {

    /** HMAC pepper，不进入仓库。 */
    private String pepper = "";

    /** 单批次最大生成数量。 */
    private int maxBatchSize = 10000;

    /** 用户维度兑换限流窗口秒数。 */
    private long redeemUserWindowSeconds = 60L;

    /** 用户维度兑换限流次数。 */
    private long redeemUserLimit = 10L;

    /** IP 维度兑换限流窗口秒数。 */
    private long redeemIpWindowSeconds = 60L;

    /** IP 维度兑换限流次数。 */
    private long redeemIpLimit = 30L;

    /** 连续失败锁定秒数。 */
    private long redeemLockSeconds = 900L;

    /** 连续失败锁定阈值。 */
    private long redeemFailureLimit = 5L;

    /** 明文 CDK 一次性导出缓存窗口秒数。 */
    private long exportWindowSeconds = 1800L;
}
