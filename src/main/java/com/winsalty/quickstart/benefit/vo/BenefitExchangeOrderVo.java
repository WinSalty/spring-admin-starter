package com.winsalty.quickstart.benefit.vo;

import lombok.Data;

/**
 * 权益兑换订单展示对象。
 * 用于用户和管理端查看兑换单处理结果。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class BenefitExchangeOrderVo {

    private String id;
    private String orderNo;
    private String userId;
    private String productId;
    private String productNo;
    private String benefitType;
    private String benefitCode;
    private Long costPoints;
    private String freezeNo;
    private String status;
    private String failureMessage;
    private String createdAt;
    private String updatedAt;
}
