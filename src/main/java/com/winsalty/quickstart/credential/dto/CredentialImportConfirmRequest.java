package com.winsalty.quickstart.credential.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 文本卡密确认导入请求。
 * 在确认阶段重新解析原文并写入批次、明细和导入任务。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialImportConfirmRequest extends CredentialImportPreviewRequest {

    @NotBlank(message = "批次名称不能为空")
    private String batchName;

    @NotBlank(message = "生效时间不能为空")
    private String validFrom;

    @NotBlank(message = "失效时间不能为空")
    private String validTo;

    private String remark;
    private Boolean createExtractLinks = false;
    private Integer itemsPerLink = 1;
    private Integer maxAccessCount = 3;
    private String expireAt;
}
