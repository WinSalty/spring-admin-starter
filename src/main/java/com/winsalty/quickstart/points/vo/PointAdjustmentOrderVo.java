package com.winsalty.quickstart.points.vo;

import lombok.Data;

/**
 * 积分人工调整单展示对象。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class PointAdjustmentOrderVo {

    private String id;
    private String adjustNo;
    private String userId;
    private String direction;
    private Long amount;
    private String status;
    private String reason;
    private String ticketNo;
    private String applicant;
    private String approver;
    private String approvedAt;
    private String createdAt;
    private String updatedAt;
}
