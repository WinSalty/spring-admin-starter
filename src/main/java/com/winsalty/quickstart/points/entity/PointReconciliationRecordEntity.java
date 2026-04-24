package com.winsalty.quickstart.points.entity;

import lombok.Data;

/**
 * 积分对账记录实体。
 * 持久化每次账户余额与流水汇总的对账结果。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class PointReconciliationRecordEntity {

    /** 主键ID。 */
    private Long id;
    /** 对账编号。 */
    private String reconcileNo;
    /** 检查账户数。 */
    private Long checkedAccounts;
    /** 差异账户数。 */
    private Long differentAccounts;
    /** 可用积分差异。 */
    private Long totalAvailableDiff;
    /** 冻结积分差异。 */
    private Long totalFrozenDiff;
    /** 状态。 */
    private String status;
    /** 检查时间。 */
    private String checkedAt;
    /** 创建时间。 */
    private String createdAt;
}
