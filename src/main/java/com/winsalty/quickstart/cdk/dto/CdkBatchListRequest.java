package com.winsalty.quickstart.cdk.dto;

import lombok.Data;

/**
 * CDK 批次列表查询请求。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class CdkBatchListRequest {

    /** 关键字。 */
    private String keyword;
    /** 批次状态。 */
    private String status;
    /** 权益类型。 */
    private String benefitType;
    /** 页码。 */
    private Integer pageNo;
    /** 每页条数。 */
    private Integer pageSize;
}
