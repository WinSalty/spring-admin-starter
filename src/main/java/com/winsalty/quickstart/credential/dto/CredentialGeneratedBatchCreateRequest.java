package com.winsalty.quickstart.credential.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 系统生成凭证批次请求。
 * 用于创建积分 CDK 等系统生成类凭证明细。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialGeneratedBatchCreateRequest {

    private Long categoryId;

    private String batchName;

    @NotNull(message = "生成数量不能为空")
    @Min(value = 1, message = "生成数量必须大于 0")
    private Integer totalCount;

    @NotNull(message = "积分数量不能为空")
    @Min(value = 1, message = "积分数量必须大于 0")
    private Long points;

    private String validFrom;

    private String validTo;

    private String remark;
}
