package com.winsalty.quickstart.cdk.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * CDK 状态变更请求。
 * 创建日期：2026-04-26
 * author：sunshengxian
 */
@Data
public class CdkCodeStatusRequest {

    /** 目标状态，仅允许 active 或 disabled。 */
    @NotBlank(message = "CDK 状态不能为空")
    private String status;
}
