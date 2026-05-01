package com.winsalty.quickstart.credential.dto;

import lombok.Data;

/**
 * 凭证批次分页请求。
 * 支持按关键字、分类、履约类型、生成模式和状态检索批次。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialBatchListRequest {

    private String keyword;
    private Long categoryId;
    private String fulfillmentType;
    private String generationMode;
    private String status;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
