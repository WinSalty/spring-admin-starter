package com.winsalty.quickstart.points.vo;

import lombok.Data;

/**
 * 积分充值单展示对象。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class PointRechargeOrderVo {

    private String id;
    private String rechargeNo;
    private String userId;
    private String channel;
    private Long amount;
    private String status;
    private String externalNo;
    private String createdAt;
    private String updatedAt;
}
