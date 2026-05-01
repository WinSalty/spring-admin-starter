package com.winsalty.quickstart.credential.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 用户凭证兑换请求。
 * 用于用户提交积分 CDK 明文并按幂等键兑换积分。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialRedeemRequest {

    @NotBlank(message = "凭证不能为空")
    private String secretText;

    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;

    private String deviceFingerprint;
}
