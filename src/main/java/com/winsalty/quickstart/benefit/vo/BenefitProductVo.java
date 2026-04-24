package com.winsalty.quickstart.benefit.vo;

import lombok.Data;

/**
 * 权益商品展示对象。
 * 用于用户兑换页和管理端商品列表展示。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class BenefitProductVo {

    private String id;
    private String productNo;
    private String productName;
    private String benefitType;
    private String benefitCode;
    private String benefitName;
    private String benefitConfig;
    private Long costPoints;
    private Integer stockTotal;
    private Integer stockUsed;
    private String validFrom;
    private String validTo;
    private String status;
    private String createdBy;
    private String createdAt;
    private String updatedAt;
}
