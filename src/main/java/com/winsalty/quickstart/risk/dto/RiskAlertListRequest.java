package com.winsalty.quickstart.risk.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 风险告警列表查询请求。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class RiskAlertListRequest {

    @Size(max = 64, message = "告警类型长度不能超过64")
    private String alertType;

    @Size(max = 32, message = "状态长度不能超过32")
    private String status;

    private Integer pageNo;

    private Integer pageSize;
}
