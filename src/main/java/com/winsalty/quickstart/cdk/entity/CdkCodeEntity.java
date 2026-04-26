package com.winsalty.quickstart.cdk.entity;

import lombok.Data;

/**
 * CDK 码实体。
 * 保存 HMAC 后码值和加密后的明文 CDK，用于兑换校验和管理端重复查看。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class CdkCodeEntity {

    /** 主键ID。 */
    private Long id;
    /** 批次ID。 */
    private Long batchId;
    /** 批次号。 */
    private String batchNo;
    /** 批次名称。 */
    private String batchName;
    /** 批次权益类型。 */
    private String benefitType;
    /** 批次权益配置 JSON。 */
    private String benefitConfig;
    /** 批次状态。 */
    private String batchStatus;
    /** 批次生效时间。 */
    private String validFrom;
    /** 批次失效时间。 */
    private String validTo;
    /** HMAC 后码值。 */
    private String codeHash;
    /** 加密后的明文 CDK。 */
    private String encryptedCode;
    /** 明文前缀。 */
    private String codePrefix;
    /** 校验位。 */
    private String checksum;
    /** 状态。 */
    private String status;
    /** 兑换用户ID。 */
    private Long redeemedUserId;
    /** 兑换时间。 */
    private String redeemedAt;
    /** 兑换记录号。 */
    private String redeemRecordNo;
    /** 乐观锁版本。 */
    private Long version;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
