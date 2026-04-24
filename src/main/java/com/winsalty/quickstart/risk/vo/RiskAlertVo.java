package com.winsalty.quickstart.risk.vo;

import lombok.Data;

/**
 * 风险告警展示对象。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class RiskAlertVo {

    private String id;
    private String alertNo;
    private String alertType;
    private String riskLevel;
    private String subjectType;
    private String subjectNo;
    private Long userId;
    private String status;
    private String detailSnapshot;
    private String handledBy;
    private String handledAt;
    private String createdAt;
    private String updatedAt;
}
