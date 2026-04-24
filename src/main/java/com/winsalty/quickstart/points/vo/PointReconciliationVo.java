package com.winsalty.quickstart.points.vo;

import lombok.Data;

/**
 * 积分对账展示对象。
 * 对比账户表余额与流水汇总余额，返回差异数量和差异积分。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class PointReconciliationVo {

    private Long checkedAccounts;
    private Long differentAccounts;
    private Long totalAvailableDiff;
    private Long totalFrozenDiff;
    private String checkedAt;
}
