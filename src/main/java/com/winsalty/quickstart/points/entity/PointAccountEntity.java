package com.winsalty.quickstart.points.entity;

import lombok.Data;

/**
 * 积分账户实体。
 * 承载用户可用积分、冻结积分和累计收支统计。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class PointAccountEntity {

    /** 主键ID。 */
    private Long id;
    /** 用户ID。 */
    private Long userId;
    /** 用户名。 */
    private String username;
    /** 用户昵称。 */
    private String nickname;
    /** 可用积分。 */
    private Long availablePoints;
    /** 冻结积分。 */
    private Long frozenPoints;
    /** 累计获得积分。 */
    private Long totalEarnedPoints;
    /** 累计消耗积分。 */
    private Long totalSpentPoints;
    /** 乐观锁版本。 */
    private Long version;
    /** 账户状态。 */
    private String status;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
