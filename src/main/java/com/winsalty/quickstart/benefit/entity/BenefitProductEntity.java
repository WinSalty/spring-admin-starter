package com.winsalty.quickstart.benefit.entity;

import lombok.Data;

/**
 * 权益商品实体。
 * 定义可用积分兑换的权限、服务包等权益及其库存、有效期和状态。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class BenefitProductEntity {

    /** 主键ID。 */
    private Long id;
    /** 商品编号。 */
    private String productNo;
    /** 商品名称。 */
    private String productName;
    /** 权益类型。 */
    private String benefitType;
    /** 权益编码。 */
    private String benefitCode;
    /** 权益名称。 */
    private String benefitName;
    /** 权益配置。 */
    private String benefitConfig;
    /** 消耗积分。 */
    private Long costPoints;
    /** 总库存。 */
    private Integer stockTotal;
    /** 已用库存。 */
    private Integer stockUsed;
    /** 生效时间。 */
    private String validFrom;
    /** 失效时间。 */
    private String validTo;
    /** 状态。 */
    private String status;
    /** 创建人。 */
    private String createdBy;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
