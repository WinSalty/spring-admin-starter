package com.winsalty.quickstart.cdk.dto;

import lombok.Data;

import javax.validation.constraints.Size;
import java.util.Map;

/**
 * CDK 公开提取访问请求。
 * 承载前端采集的浏览器指纹摘要和设备快照，用于访问审计。
 * 创建日期：2026-04-30
 * author：sunshengxian
 */
@Data
public class CdkExtractAccessRequest {

    /** 浏览器指纹摘要。 */
    @Size(max = 128, message = "浏览器指纹长度不能超过 128 个字符")
    private String browserFingerprint;

    /** 设备快照。 */
    private Map<String, Object> deviceSnapshot;
}
