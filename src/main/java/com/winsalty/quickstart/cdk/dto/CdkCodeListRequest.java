package com.winsalty.quickstart.cdk.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * CDK 明细列表查询请求。
 * 创建日期：2026-04-26
 * author：sunshengxian
 */
@Data
public class CdkCodeListRequest {

    /** 批次ID。 */
    @NotNull(message = "批次ID不能为空")
    private Long batchId;

    /** CDK 状态。 */
    private String status;

    /** 页码。 */
    private Integer pageNo;

    /** 每页数量。 */
    private Integer pageSize;
}
