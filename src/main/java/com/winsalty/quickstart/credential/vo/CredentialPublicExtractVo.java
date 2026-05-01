package com.winsalty.quickstart.credential.vo;

import lombok.Data;

import java.util.List;

/**
 * 公开凭证提取结果。
 * 展示链接、分类、批次、剩余访问次数和可复制凭证明细。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialPublicExtractVo {

    private String linkNo;
    private String categoryName;
    private String fulfillmentType;
    private String batchName;
    private Integer remainingAccessCount;
    private String expireAt;
    private List<CredentialPublicExtractItemVo> items;
}
