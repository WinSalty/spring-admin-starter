package com.winsalty.quickstart.points.dto;

import lombok.Data;

/**
 * 积分变更命令。
 * 所有积分充值、扣减、冻结、解冻和退款都通过该命令进入账务服务。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class PointChangeCommand {

    /** 用户ID。 */
    private Long userId;
    /** 变更积分。 */
    private Long amount;
    /** 业务类型。 */
    private String bizType;
    /** 业务单号。 */
    private String bizNo;
    /** 幂等键。 */
    private String idempotencyKey;
    /** 操作人类型。 */
    private String operatorType;
    /** 操作人ID。 */
    private String operatorId;
    /** 备注。 */
    private String remark;
}
