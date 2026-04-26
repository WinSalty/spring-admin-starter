package com.winsalty.quickstart.cdk.dto;

import lombok.Data;

/**
 * CDK 明细列表查询请求。
 * 创建日期：2026-04-26
 * author：sunshengxian
 */
@Data
public class CdkCodeListRequest {

    /** 关键字，支持批次号、批次名称和 CDK 前缀查询。 */
    private String keyword;

    /** 批次ID，不传时查询全部 CDK。 */
    private Long batchId;

    /** CDK 状态。 */
    private String status;

    /** 页码。 */
    private Integer pageNo;

    /** 每页数量。 */
    private Integer pageSize;
}
