package com.winsalty.quickstart.points.entity;

import lombok.Data;

/**
 * 积分人工调整单实体。
 * 管理员调整必须先建单再审批，审批通过后才写入账本。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class PointAdjustmentOrderEntity {

    /** 主键ID。 */
    private Long id;
    /** 调整单号。 */
    private String adjustNo;
    /** 用户ID。 */
    private Long userId;
    /** 调整方向。 */
    private String direction;
    /** 调整积分。 */
    private Long amount;
    /** 状态。 */
    private String status;
    /** 申请原因。 */
    private String reason;
    /** 工单号或附件引用。 */
    private String ticketNo;
    /** 幂等键。 */
    private String idempotencyKey;
    /** 申请人。 */
    private String applicant;
    /** 审批人。 */
    private String approver;
    /** 审批时间。 */
    private String approvedAt;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
