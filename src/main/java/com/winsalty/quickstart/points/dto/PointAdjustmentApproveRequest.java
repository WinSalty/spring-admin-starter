package com.winsalty.quickstart.points.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 积分人工调整审批请求。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class PointAdjustmentApproveRequest {

    /** 是否通过审批。 */
    @NotNull(message = "审批结果不能为空")
    private Boolean approved;
}
