package com.winsalty.quickstart.cdk.vo;

import lombok.Data;

/**
 * CDK 兑换记录展示对象。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class CdkRedeemRecordVo {

    private String id;
    private String redeemNo;
    private String userId;
    private String batchId;
    private String codeId;
    private String benefitType;
    private String benefitSnapshot;
    private String status;
    private String failureCode;
    private String failureMessage;
    private String clientIp;
    private String traceId;
    private String createdAt;
    private String updatedAt;
}
