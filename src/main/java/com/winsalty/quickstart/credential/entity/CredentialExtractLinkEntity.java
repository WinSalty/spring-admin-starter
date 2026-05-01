package com.winsalty.quickstart.credential.entity;

import lombok.Data;

/**
 * 凭证提取链接实体。
 * 记录提取链接、访问次数、过期时间、状态和管理端备注。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialExtractLinkEntity {

    /** 主键ID。 */
    private Long id;
    /** 链接编号。 */
    private String linkNo;
    /** 分类ID。 */
    private Long categoryId;
    /** 分类名称。 */
    private String categoryName;
    /** 批次ID。 */
    private Long batchId;
    /** 批次号。 */
    private String batchNo;
    /** 批次名称。 */
    private String batchName;
    /** Token HMAC。 */
    private String tokenHash;
    /** 加密后的 Token。 */
    private String encryptedToken;
    /** Token 密钥版本。 */
    private String tokenKeyId;
    /** 包含凭证数量。 */
    private Integer itemCount;
    /** 最大成功访问次数。 */
    private Integer maxAccessCount;
    /** 已成功访问次数。 */
    private Integer accessedCount;
    /** 过期时间。 */
    private String expireAt;
    /** 状态。 */
    private String status;
    /** 创建人ID。 */
    private Long createdBy;
    /** 停用人ID。 */
    private Long disabledBy;
    /** 停用时间。 */
    private String disabledAt;
    /** 最近成功访问时间。 */
    private String lastAccessedAt;
    /** 备注。 */
    private String remark;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
