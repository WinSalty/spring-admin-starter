package com.winsalty.quickstart.credential.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 凭证中心配置。
 * 统一接收凭证明文安全、导入限制、公开提取链接和兑换限流配置。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.credential")
public class CredentialProperties {

    /** 凭证明文 HMAC pepper，不进入仓库。 */
    private String secretPepper = "";

    /** 凭证明文 AES-GCM 加密密钥，不进入仓库。 */
    private String secretEncryptionKey = "";

    /** 单个系统生成凭证批次最大数量。 */
    private int maxBatchSize = 10000;

    /** 文本导入最大凭证数量。 */
    private int importMaxItems = 10000;

    /** 文本导入最大原文大小，单位字节。 */
    private int importMaxTextBytes = 1048576;

    /** 用户维度兑换限流窗口，单位秒。 */
    private long redeemUserWindowSeconds = 60L;

    /** 用户维度兑换限流次数。 */
    private long redeemUserLimit = 10L;

    /** IP 维度兑换限流窗口，单位秒。 */
    private long redeemIpWindowSeconds = 60L;

    /** IP 维度兑换限流次数。 */
    private long redeemIpLimit = 30L;

    /** 连续兑换失败后用户锁定时间，单位秒。 */
    private long redeemLockSeconds = 900L;

    /** 连续兑换失败锁定阈值。 */
    private long redeemFailureLimit = 5L;

    /** 凭证公开提取链接配置。 */
    private Extract extract = new Extract();

    /**
     * 凭证公开提取链接配置。
     * 创建日期：2026-05-01
     * author：sunshengxian
     */
    @Data
    public static class Extract {

        /** 公开提取页面基础 URL。 */
        private String publicBaseUrl = "http://localhost:5173";

        /** 提取 token HMAC 密钥，不进入仓库。 */
        private String tokenSecret = "";

        /** 提取 token AES-GCM 加密密钥，用于管理端重复复制 URL。 */
        private String tokenEncryptionKey = "";

        /** 单链接最大成功访问次数。 */
        private int maxAccessCount = 100;

        /** 单链接最长有效天数。 */
        private int maxExpireDays = 30;

        /** 单链接最多包含凭证数量。 */
        private int maxItemsPerLink = 50;
    }
}
