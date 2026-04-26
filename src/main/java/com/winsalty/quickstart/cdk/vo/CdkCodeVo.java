package com.winsalty.quickstart.cdk.vo;

import lombok.Data;

/**
 * CDK 明细展示对象。
 * 创建日期：2026-04-26
 * author：sunshengxian
 */
@Data
public class CdkCodeVo {

    private String id;
    private String batchId;
    private String batchNo;
    private String batchName;
    private String benefitType;
    private String benefitConfig;
    private String batchStatus;
    private String validFrom;
    private String validTo;
    private String cdk;
    private String codePrefix;
    private String checksum;
    private String status;
    private String redeemedUserId;
    private String redeemedAt;
    private String redeemRecordNo;
    private String createdAt;
    private String updatedAt;
}
