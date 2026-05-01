package com.winsalty.quickstart.credential.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 凭证提取链接创建请求。
 * 支持按批次批量生成链接或按单个明细生成链接。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialExtractLinkCreateRequest {

    @NotNull(message = "每个链接包含数量不能为空")
    @Min(value = 1, message = "每个链接至少包含 1 条凭证")
    private Integer itemsPerLink = 1;

    @NotNull(message = "最大访问次数不能为空")
    @Min(value = 1, message = "最大访问次数至少为 1")
    private Integer maxAccessCount = 3;

    @NotBlank(message = "过期时间不能为空")
    private String expireAt;

    private String itemScope = "UNLINKED_ACTIVE";

    @Size(max = 255, message = "备注不能超过 255 个字符")
    private String remark;
}
