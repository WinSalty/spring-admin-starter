package com.winsalty.quickstart.cdk.dto;

import lombok.Data;

/**
 * CDK 兑换记录查询请求。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class CdkRedeemRecordListRequest {

    /** 用户ID。 */
    private Long userId;
    /** 批次ID。 */
    private Long batchId;
    /** 状态。 */
    private String status;
    /** 页码。 */
    private Integer pageNo;
    /** 每页条数。 */
    private Integer pageSize;
}
