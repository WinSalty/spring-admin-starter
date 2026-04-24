package com.winsalty.quickstart.trade.vo;

import lombok.Data;

/**
 * 在线充值订单视图。
 * 返回充值单状态和后续支付渠道扩展所需的稳定业务编号。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class OnlineRechargeVo {

    /** 充值单号。 */
    private String rechargeNo;
    /** 充值渠道。 */
    private String channel;
    /** 充值积分。 */
    private Long amount;
    /** 状态。 */
    private String status;
    /** 外部流水号。 */
    private String externalNo;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
