package com.winsalty.quickstart.points.entity;

import lombok.Data;

/**
 * 积分账本流水实体。
 * 账本只追加不物理删除，记录每次积分变更前后余额和哈希链信息。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class PointLedgerEntity {

    /** 主键ID。 */
    private Long id;
    /** 流水号。 */
    private String ledgerNo;
    /** 用户ID。 */
    private Long userId;
    /** 积分账户ID。 */
    private Long accountId;
    /** 变更方向。 */
    private String direction;
    /** 变更积分。 */
    private Long amount;
    /** 变更前可用余额。 */
    private Long balanceBefore;
    /** 变更后可用余额。 */
    private Long balanceAfter;
    /** 变更前冻结余额。 */
    private Long frozenBefore;
    /** 变更后冻结余额。 */
    private Long frozenAfter;
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
    /** 链路ID。 */
    private String traceId;
    /** 上一条流水哈希。 */
    private String prevHash;
    /** 当前流水哈希。 */
    private String entryHash;
    /** 备注。 */
    private String remark;
    /** 创建时间。 */
    private String createdAt;
}
