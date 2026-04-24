package com.winsalty.quickstart.cdk.vo;

import lombok.Data;

/**
 * CDK 批次展示对象。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class CdkBatchVo {

    private String id;
    private String batchNo;
    private String batchName;
    private String benefitType;
    private String benefitConfig;
    private Integer totalCount;
    private Integer generatedCount;
    private Integer redeemedCount;
    private String validFrom;
    private String validTo;
    private String status;
    private String riskLevel;
    private String createdBy;
    private String approvedBy;
    private String approvedAt;
    private String secondApprovedBy;
    private String secondApprovedAt;
    private Integer exportCount;
    private String createdAt;
    private String updatedAt;
}
