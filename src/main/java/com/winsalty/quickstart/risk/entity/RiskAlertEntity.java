package com.winsalty.quickstart.risk.entity;

import lombok.Data;

/**
 * 风险告警实体。
 * 记录异常兑换、双人复核等运营风控事件，供管理端追踪处理。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class RiskAlertEntity {

    /** 主键ID。 */
    private Long id;
    /** 告警编号。 */
    private String alertNo;
    /** 告警类型。 */
    private String alertType;
    /** 风险等级。 */
    private String riskLevel;
    /** 对象类型。 */
    private String subjectType;
    /** 对象编号。 */
    private String subjectNo;
    /** 用户ID。 */
    private Long userId;
    /** 状态。 */
    private String status;
    /** 告警详情。 */
    private String detailSnapshot;
    /** 处理人。 */
    private String handledBy;
    /** 处理时间。 */
    private String handledAt;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
