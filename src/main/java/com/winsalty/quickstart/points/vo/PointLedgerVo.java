package com.winsalty.quickstart.points.vo;

import lombok.Data;

/**
 * 积分流水展示对象。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class PointLedgerVo {

    private String id;
    private String ledgerNo;
    private String userId;
    private String accountId;
    private String direction;
    private Long amount;
    private Long balanceBefore;
    private Long balanceAfter;
    private Long frozenBefore;
    private Long frozenAfter;
    private String bizType;
    private String bizNo;
    private String operatorType;
    private String operatorId;
    private String traceId;
    private String entryHash;
    private String remark;
    private String createdAt;
}
