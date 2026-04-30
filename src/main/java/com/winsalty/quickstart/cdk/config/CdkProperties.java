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

    /** CDK 临时提取链接配置。 */
    private Extract extract = new Extract();

    /**
     * CDK 临时提取链接配置。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    @Data
    public static class Extract {

        /** 公开提取页面基础 URL。 */
        private String publicBaseUrl = "http://localhost:5173";

        /** 提取 token 摘要密钥，不进入仓库。 */
        private String tokenSecret = "";

        /** 单链接最大访问次数上限。 */
        private int maxAccessCount = 100;

        /** 单链接最长有效天数。 */
        private int maxExpireDays = 30;
    }
}
