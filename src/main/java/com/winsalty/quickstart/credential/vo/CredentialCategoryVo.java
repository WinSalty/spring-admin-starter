package com.winsalty.quickstart.credential.vo;

import lombok.Data;

/**
 * 凭证分类展示对象。
 * 用于管理端分类列表、筛选下拉和分类配置页展示。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialCategoryVo {

    private String id;
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
