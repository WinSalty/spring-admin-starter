package com.winsalty.quickstart.credential.dto;

import lombok.Data;

/**
 * 公开凭证提取请求。
 * 记录前端采集的浏览器指纹和设备摘要，便于访问审计和风控分析。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialPublicExtractRequest {

    private String browserFingerprint;
    private String deviceSnapshot;
}
