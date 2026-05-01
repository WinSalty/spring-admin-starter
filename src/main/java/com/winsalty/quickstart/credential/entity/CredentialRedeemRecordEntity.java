package com.winsalty.quickstart.credential.entity;

import lombok.Data;

/**
 * 凭证兑换记录实体。
 * 记录积分 CDK 兑换流水、幂等键和积分账本流水号。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialRedeemRecordEntity {

    private Long id;
    private String recordNo;
    private Long itemId;
    private Long batchId;
    private Long categoryId;
    private Long userId;
    private Long points;
    private String idempotencyKey;
    private String clientIp;
    private String userAgentHash;
    private String deviceFingerprint;
    private String ledgerNo;
    private String status;
    private String failureReason;
    private String createdAt;
}
