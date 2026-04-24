package com.winsalty.quickstart.points.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 积分人工调整申请请求。
 * 管理员必须填写调整方向、原因和工单引用，审批后才会入账。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class PointAdjustmentRequest {

    /** 被调整用户ID。 */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 调整方向：earn 或 spend。 */
    @NotBlank(message = "调整方向不能为空")
    private String direction;

    /** 调整积分。 */
    @NotNull(message = "调整积分不能为空")
    @Min(value = 1L, message = "调整积分必须大于 0")
    private Long amount;

    /** 调整原因。 */
    @NotBlank(message = "调整原因不能为空")
    private String reason;

    /** 工单号或附件引用。 */
    @NotBlank(message = "工单号不能为空")
    private String ticketNo;

    /** 幂等键。 */
    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;
}
