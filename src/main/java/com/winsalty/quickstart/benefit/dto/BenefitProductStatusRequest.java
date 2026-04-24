package com.winsalty.quickstart.benefit.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 权益商品状态变更请求。
 * 管理端使用该请求启用或停用权益商品。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class BenefitProductStatusRequest {

    /** 商品状态。 */
    @NotBlank(message = "状态不能为空")
    private String status;
}
