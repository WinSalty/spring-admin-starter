package com.winsalty.quickstart.credential.vo;

import lombok.Data;

/**
 * 凭证兑换记录展示对象。
 * 展示积分 CDK 兑换流水、入账积分和账本流水号。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialRedeemRecordVo {

    private String id;
    private String recordNo;
    private String itemId;
    private String batchId;
    private String categoryId;
    private String userId;
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
