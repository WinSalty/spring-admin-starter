package com.winsalty.quickstart.cdk.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * CDK 批次创建请求。
 * 首期仅开放积分权益，后续权益类型通过明确字段扩展，避免任意 JSON 直通执行。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class CdkBatchCreateRequest {

    /** 批次名称。 */
    @NotBlank(message = "批次名称不能为空")
    private String batchName;

    /** 权益类型。 */
    @NotBlank(message = "权益类型不能为空")
    private String benefitType;

    /** 积分权益数量。 */
    @NotNull(message = "积分数量不能为空")
    @Min(value = 1L, message = "积分数量必须大于 0")
    private Long points;

    /** CDK 数量。 */
    @NotNull(message = "生成数量不能为空")
    @Min(value = 1L, message = "生成数量必须大于 0")
    private Integer totalCount;

    /** 生效时间。 */
    @NotBlank(message = "生效时间不能为空")
    private String validFrom;

    /** 失效时间。 */
    @NotBlank(message = "失效时间不能为空")
    private String validTo;

    /** 风险等级。 */
    private String riskLevel;

    /** 备注。 */
    private String remark;
}
