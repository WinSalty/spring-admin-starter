package com.winsalty.quickstart.credential.entity;

import lombok.Data;

/**
 * 凭证批次实体。
 * 统一承载系统生成积分 CDK 和导入文本卡密批次。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialBatchEntity {

    private Long id;
    private String batchNo;
    private String batchName;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String fulfillmentType;
    private String generationMode;
    private String payloadConfig;
    private Integer totalCount;
    private Integer availableCount;
    private Integer consumedCount;
    private Integer linkedCount;
    private String validFrom;
    private String validTo;
    private String status;
    private Long createdBy;
    private String createdAt;
    private String updatedAt;
}
