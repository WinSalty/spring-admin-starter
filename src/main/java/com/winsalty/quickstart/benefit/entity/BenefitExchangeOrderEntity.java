package com.winsalty.quickstart.benefit.entity;

import lombok.Data;

/**
 * 权益兑换订单实体。
 * 记录用户积分冻结、权益发放和确认扣减的完整链路。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class BenefitExchangeOrderEntity {

    /** 主键ID。 */
    private Long id;
    /** 兑换单号。 */
    private String orderNo;
    /** 用户ID。 */
    private Long userId;
    /** 商品ID。 */
    private Long productId;
    /** 商品编号。 */
    private String productNo;
    /** 权益类型。 */
    private String benefitType;
    /** 权益编码。 */
    private String benefitCode;
    /** 消耗积分。 */
    private Long costPoints;
    /** 冻结单号。 */
    private String freezeNo;
    /** 状态。 */
    private String status;
    /** 失败原因。 */
    private String failureMessage;
    /** 幂等键。 */
    private String idempotencyKey;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
