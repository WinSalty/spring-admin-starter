package com.winsalty.quickstart.benefit.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 权益兑换请求。
 * 用户提交幂等键后使用积分兑换指定权益商品。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class BenefitExchangeRequest {

    /** 客户端幂等键。 */
    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;
}
