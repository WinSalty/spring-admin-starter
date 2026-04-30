package com.winsalty.quickstart.cdk.vo;

import lombok.Data;

/**
 * CDK 提取链接展示对象。
 * 用于管理端展示链接状态、访问次数和创建结果中的一次性 URL。
 * 创建日期：2026-04-30
 * author：sunshengxian
 */
@Data
public class CdkExtractLinkVo {

    private String id;
    private String linkNo;
    private String codeId;
    private String batchId;
    private String url;
    private Integer maxAccessCount;
    private Integer accessedCount;
    private Integer remainingAccessCount;
    private String expireAt;
    private String status;
    private String createdBy;
    private String disabledBy;
    private String disabledAt;
    private String remark;
    private String lastAccessedAt;
    private String createdAt;
    private String updatedAt;
}
