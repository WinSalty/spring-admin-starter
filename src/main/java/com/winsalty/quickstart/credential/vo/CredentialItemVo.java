package com.winsalty.quickstart.credential.vo;

import lombok.Data;

/**
 * 凭证明细展示对象。
 * 管理端凭证明细展示明文和状态，便于批次内复制与排查。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialItemVo {

    private String id;
    private String batchId;
    private String categoryId;
    private String itemNo;
    private String secretText;
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
