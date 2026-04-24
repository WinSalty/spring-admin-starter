package com.winsalty.quickstart.trade.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 交易模块配置。
 * 维护在线充值回调密钥、回调时间窗口和单笔充值上限。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.trade")
public class TradeProperties {

    /** 支付回调 HMAC 密钥，不进入仓库。 */
    private String callbackSecret = "";

    /** 回调时间戳允许偏移秒数。 */
    private long callbackSkewSeconds = 300L;

    /** 单笔在线充值最大积分。 */
    private long maxRechargePoints = 100000L;
}
