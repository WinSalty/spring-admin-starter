package com.winsalty.quickstart.cdk.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 用户 CDK 兑换请求。
 * 明文 CDK 仅用于本次校验和 HMAC，不写入数据库或日志。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class CdkRedeemRequest {

    /** CDK 明文。 */
    @NotBlank(message = "CDK 不能为空")
    private String cdk;

    /** 客户端幂等键。 */
    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;
}
