package com.winsalty.quickstart.points.entity;

import lombok.Data;

/**
 * 积分充值单实体。
 * 先记录业务单据，再调用积分账务服务入账，保障审计可追溯。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class PointRechargeOrderEntity {

    /** 主键ID。 */
    private Long id;
    /** 充值单号。 */
    private String rechargeNo;
    /** 用户ID。 */
    private Long userId;
    /** 充值渠道。 */
    private String channel;
    /** 充值积分。 */
    private Long amount;
    /** 订单状态。 */
    private String status;
    /** 外部业务流水。 */
    private String externalNo;
    /** 幂等键。 */
    private String idempotencyKey;
    /** 请求摘要。 */
    private String requestSnapshot;
    /** 结果摘要。 */
    private String resultSnapshot;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
