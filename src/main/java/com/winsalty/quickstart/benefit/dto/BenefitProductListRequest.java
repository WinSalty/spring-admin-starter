package com.winsalty.quickstart.benefit.dto;

import lombok.Data;

/**
 * 权益商品列表查询请求。
 * 支持按关键字、权益类型和状态筛选。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class BenefitProductListRequest {

    /** 关键字。 */
    private String keyword;
    /** 权益类型。 */
    private String benefitType;
    /** 状态。 */
    private String status;
    /** 当前页。 */
    private Integer pageNo;
    /** 每页条数。 */
    private Integer pageSize;
}
