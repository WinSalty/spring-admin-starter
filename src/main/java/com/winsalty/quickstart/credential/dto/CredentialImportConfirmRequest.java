package com.winsalty.quickstart.credential.dto;

import lombok.Data;

/**
 * 文本卡密确认导入请求。
 * 在确认阶段重新解析原文并写入批次、明细和导入任务。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialImportConfirmRequest extends CredentialImportPreviewRequest {

    private String batchName;

    private String validFrom;

    private String validTo;

    private String remark;
    private Boolean createExtractLinks = false;
    private Integer itemsPerLink = 1;
    private Integer maxAccessCount = 3;
    private String itemScope = com.winsalty.quickstart.credential.constant.CredentialConstants.ITEM_SCOPE_UNLINKED_ACTIVE;
    private String expireAt;
}
