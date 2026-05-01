package com.winsalty.quickstart.credential.entity;

import lombok.Data;

/**
 * 凭证分类实体。
 * 保存凭证分类、履约类型、生成模式和安全策略配置。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialCategoryEntity {

    private Long id;
    private String categoryCode;
    private String categoryName;
    private String fulfillmentType;
    private String generationMode;
    private String payloadSchema;
    private String importConfig;
    private String extractPolicy;
    private String status;
    private String createdAt;
    private String updatedAt;
}
