package com.winsalty.quickstart.benefit.dto;

import lombok.Data;

/**
 * 权益兑换订单列表查询请求。
 * 支持用户、权益类型、状态筛选。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class BenefitOrderListRequest {

    /** 用户ID。 */
    private Long userId;
    /** 权益类型。 */
    private String benefitType;
    /** 状态。 */
    private String status;
    /** 当前页。 */
    private Integer pageNo;
    /** 每页条数。 */
    private Integer pageSize;
}
