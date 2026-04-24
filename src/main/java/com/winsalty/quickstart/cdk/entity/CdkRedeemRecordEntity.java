package com.winsalty.quickstart.cdk.entity;

import lombok.Data;

/**
 * CDK 兑换记录实体。
 * 记录兑换链路、权益快照、失败原因和幂等键，便于追溯。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class CdkRedeemRecordEntity {

    /** 主键ID。 */
    private Long id;
    /** 兑换记录号。 */
    private String redeemNo;
    /** 用户ID。 */
    private Long userId;
    /** 批次ID。 */
    private Long batchId;
    /** CDK ID。 */
    private Long codeId;
    /** 权益类型。 */
    private String benefitType;
    /** 权益快照。 */
    private String benefitSnapshot;
    /** 状态。 */
    private String status;
    /** 失败码。 */
    private String failureCode;
    /** 失败原因。 */
    private String failureMessage;
    /** 客户端 IP。 */
    private String clientIp;
    /** UA 摘要。 */
    private String userAgentHash;
    /** 链路ID。 */
    private String traceId;
    /** 幂等键。 */
    private String idempotencyKey;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
