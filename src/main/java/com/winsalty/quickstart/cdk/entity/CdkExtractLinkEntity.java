package com.winsalty.quickstart.cdk.entity;

import lombok.Data;

/**
 * CDK 提取链接实体。
 * 保存临时提取 URL 的 token 摘要、访问次数、状态和审计创建信息。
 * 创建日期：2026-04-30
 * author：sunshengxian
 */
@Data
public class CdkExtractLinkEntity {

    /** 主键ID。 */
    private Long id;
    /** 提取链接编号。 */
    private String linkNo;
    /** CDK ID。 */
    private Long codeId;
    /** 批次ID。 */
    private Long batchId;
    /** token 摘要。 */
    private String tokenHash;
    /** 最大访问次数。 */
    private Integer maxAccessCount;
    /** 已访问次数。 */
    private Integer accessedCount;
    /** 过期时间。 */
    private String expireAt;
    /** 状态。 */
    private String status;
    /** 创建人。 */
    private String createdBy;
    /** 停用人。 */
    private String disabledBy;
    /** 停用时间。 */
    private String disabledAt;
    /** 备注。 */
    private String remark;
    /** 最近访问时间。 */
    private String lastAccessedAt;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
