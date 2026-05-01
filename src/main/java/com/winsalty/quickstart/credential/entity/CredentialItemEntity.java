package com.winsalty.quickstart.credential.entity;

import lombok.Data;

/**
 * 凭证明细实体。
 * 承载系统生成兑换码和导入文本卡密的统一明细数据。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialItemEntity {

    /** 主键ID。 */
    private Long id;
    /** 批次ID。 */
    private Long batchId;
    /** 分类ID。 */
    private Long categoryId;
    /** 明细编号。 */
    private String itemNo;
    /** 脱敏展示值。 */
    private String secretMask;
    /** 校验位或短摘要。 */
    private String checksum;
    /** 来源类型。 */
    private String sourceType;
    /** 状态。 */
    private String status;
    /** 兑换用户ID。 */
    private Long consumedUserId;
    /** 消费时间。 */
    private String consumedAt;
    /** 消费业务单号。 */
    private String consumeBizNo;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
