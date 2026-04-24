package com.winsalty.quickstart.points.entity;

import lombok.Data;

/**
 * 积分冻结单实体。
 * 支持后续权益兑换或服务购买的二阶段扣减流程。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class PointFreezeOrderEntity {

    /** 主键ID。 */
    private Long id;
    /** 冻结单号。 */
    private String freezeNo;
    /** 用户ID。 */
    private Long userId;
    /** 冻结积分。 */
    private Long amount;
    /** 业务类型。 */
    private String bizType;
    /** 业务单号。 */
    private String bizNo;
    /** 状态。 */
    private String status;
    /** 过期时间。 */
    private String expireAt;
    /** 幂等键。 */
    private String idempotencyKey;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
