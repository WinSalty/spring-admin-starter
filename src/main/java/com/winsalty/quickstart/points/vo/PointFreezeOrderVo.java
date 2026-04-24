package com.winsalty.quickstart.points.vo;

import lombok.Data;

/**
 * 积分冻结单展示对象。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class PointFreezeOrderVo {

    private String id;
    private String freezeNo;
    private String userId;
    private Long amount;
    private String bizType;
    private String bizNo;
    private String status;
    private String expireAt;
    private String createdAt;
    private String updatedAt;
}
