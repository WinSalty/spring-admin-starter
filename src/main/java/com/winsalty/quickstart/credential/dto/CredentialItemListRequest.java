package com.winsalty.quickstart.credential.dto;

import lombok.Data;

/**
 * 凭证明细分页请求。
 * 支持按批次、分类、来源、状态和脱敏关键字查询凭证明细。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialItemListRequest {

    private String keyword;
    private Long batchId;
    private Long categoryId;
    private String sourceType;
    private String status;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
