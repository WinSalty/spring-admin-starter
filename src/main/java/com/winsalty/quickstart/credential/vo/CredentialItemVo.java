package com.winsalty.quickstart.credential.vo;

import lombok.Data;

/**
 * 凭证明细展示对象。
 * 链接详情中默认只展示脱敏值和状态，不暴露明文凭证。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialItemVo {

    private String id;
    private String batchId;
    private String categoryId;
    private String itemNo;
    private String secretMask;
    private String checksum;
    private String sourceType;
    private String status;
    private String consumedUserId;
    private String consumedAt;
    private String consumeBizNo;
    private String createdAt;
    private String updatedAt;
}
