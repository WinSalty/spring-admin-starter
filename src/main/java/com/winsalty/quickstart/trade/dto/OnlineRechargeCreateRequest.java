package com.winsalty.quickstart.trade.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 在线充值创建请求。
 * 当前只创建本地充值单，真实支付渠道可基于返回的充值单号扩展支付参数。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class OnlineRechargeCreateRequest {

    /** 充值积分。 */
    @NotNull(message = "充值积分不能为空")
    @Min(value = 1, message = "充值积分必须大于0")
    private Long amount;

    /** 客户端幂等键。 */
    @NotBlank(message = "幂等键不能为空")
    @Size(max = 128, message = "幂等键长度不能超过128")
    private String idempotencyKey;
}
