package com.winsalty.quickstart.credential.vo;

import lombok.Data;

/**
 * 凭证批次展示对象。
 * 汇总批次数量、分类、履约方式、有效期和状态。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialBatchVo {

    private String id;
    private String batchNo;
    private String batchName;
    private String categoryId;
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
    private String createdBy;
    private String createdAt;
    private String updatedAt;
}
