package com.winsalty.quickstart.credential.vo;

import lombok.Data;

/**
 * 凭证提取链接展示对象。
 * 汇总链接基础信息、分类、批次、访问进度和可复制状态。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialExtractLinkVo {

    private String id;
    private String linkNo;
    private String categoryId;
    private String categoryName;
    private String batchId;
    private String batchNo;
    private String batchName;
    private Integer itemCount;
    private Integer maxAccessCount;
    private Integer accessedCount;
    private Integer remainingAccessCount;
    private String expireAt;
    private String status;
    private String createdBy;
    private String disabledBy;
    private String disabledAt;
    private String lastAccessedAt;
    private String remark;
    private Boolean copyable;
    private String createdAt;
    private String updatedAt;
}
