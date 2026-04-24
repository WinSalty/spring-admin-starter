package com.winsalty.quickstart.benefit.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 权益商品保存请求。
 * 管理端通过该请求创建或更新积分兑换商品。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class BenefitProductSaveRequest {

    /** 商品名称。 */
    @NotBlank(message = "商品名称不能为空")
    private String productName;
    /** 权益类型。 */
    @NotBlank(message = "权益类型不能为空")
    private String benefitType;
    /** 权益编码。 */
    @NotBlank(message = "权益编码不能为空")
    private String benefitCode;
    /** 权益名称。 */
    @NotBlank(message = "权益名称不能为空")
    private String benefitName;
    /** 权益配置。 */
    private String benefitConfig;
    /** 消耗积分。 */
    @NotNull(message = "消耗积分不能为空")
    @Min(value = 1L, message = "消耗积分必须大于0")
    private Long costPoints;
    /** 总库存，-1 表示不限。 */
    @NotNull(message = "库存不能为空")
    private Integer stockTotal;
    /** 生效时间。 */
    @NotBlank(message = "生效时间不能为空")
    private String validFrom;
    /** 失效时间。 */
    @NotBlank(message = "失效时间不能为空")
    private String validTo;
    /** 状态。 */
    private String status;
}
