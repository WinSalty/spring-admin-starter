package com.winsalty.quickstart.credential.dto;

import lombok.Data;

/**
 * 凭证兑换记录分页请求。
 * 支持按用户、批次和状态查询积分 CDK 兑换流水。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialRedeemRecordListRequest {

    private Long userId;
    private Long batchId;
    private String status;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
