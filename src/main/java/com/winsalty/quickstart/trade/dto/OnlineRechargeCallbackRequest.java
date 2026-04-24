package com.winsalty.quickstart.trade.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 在线充值支付回调请求。
 * 回调方需按约定字段计算 HMAC 签名，后端验签后再推进充值单状态和积分入账。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class OnlineRechargeCallbackRequest {

    /** 本地充值单号。 */
    @NotBlank(message = "充值单号不能为空")
    @Size(max = 64, message = "充值单号长度不能超过64")
    private String rechargeNo;

    /** 第三方支付流水号。 */
    @NotBlank(message = "外部流水号不能为空")
    @Size(max = 128, message = "外部流水号长度不能超过128")
    private String externalNo;

    /** 支付结果状态。 */
    @NotBlank(message = "支付状态不能为空")
    @Size(max = 32, message = "支付状态长度不能超过32")
    private String status;

    /** 充值积分。 */
    @NotNull(message = "充值积分不能为空")
    @Min(value = 1, message = "充值积分必须大于0")
    private Long amount;

    /** 回调时间戳，毫秒。 */
    @NotNull(message = "回调时间戳不能为空")
    private Long timestamp;

    /** 回调随机串。 */
    @NotBlank(message = "回调随机串不能为空")
    @Size(max = 64, message = "回调随机串长度不能超过64")
    private String nonce;

    /** HMAC 签名。 */
    @NotBlank(message = "回调签名不能为空")
    @Size(max = 128, message = "回调签名长度不能超过128")
    private String signature;
}
