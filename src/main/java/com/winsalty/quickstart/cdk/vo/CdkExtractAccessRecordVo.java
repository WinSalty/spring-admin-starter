package com.winsalty.quickstart.cdk.vo;

import lombok.Data;

/**
 * CDK 提取访问记录展示对象。
 * 用于管理端查看临时 URL 的访问审计明细。
 * 创建日期：2026-04-30
 * author：sunshengxian
 */
@Data
public class CdkExtractAccessRecordVo {

    private String id;
    private String accessNo;
    private String linkId;
    private String codeId;
    private String batchId;
    private String result;
    private String failureCode;
    private String failureMessage;
    private String clientIp;
    private String userAgentHash;
    private String browserFingerprint;
    private String deviceSnapshot;
    private String referer;
    private String traceId;
    private String createdAt;
}
