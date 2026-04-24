package com.winsalty.quickstart.cdk.entity;

import lombok.Data;

/**
 * CDK 码实体。
 * 只保存 HMAC 后码值和运营排查用前缀，不保存明文 CDK。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class CdkCodeEntity {

    /** 主键ID。 */
    private Long id;
    /** 批次ID。 */
    private Long batchId;
    /** HMAC 后码值。 */
    private String codeHash;
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
