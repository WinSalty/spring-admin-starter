package com.winsalty.quickstart.points.vo;

import lombok.Data;

/**
 * 积分账户展示对象。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class PointAccountVo {

    private String id;
    private String userId;
    private String username;
    private String nickname;
    private Long availablePoints;
    private Long frozenPoints;
    private Long totalEarnedPoints;
    private Long totalSpentPoints;
    private String status;
    private String createdAt;
    private String updatedAt;
}
