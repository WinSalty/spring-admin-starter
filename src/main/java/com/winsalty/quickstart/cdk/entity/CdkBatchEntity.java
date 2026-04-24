package com.winsalty.quickstart.cdk.entity;

import lombok.Data;

/**
 * CDK 批次实体。
 * 记录批次权益配置、生成数量、兑换统计和状态流转信息。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class CdkBatchEntity {

    /** 主键ID。 */
    private Long id;
    /** 批次号。 */
    private String batchNo;
    /** 批次名称。 */
    private String batchName;
    /** 权益类型。 */
    private String benefitType;
    /** 权益配置 JSON。 */
    private String benefitConfig;
    /** 总数量。 */
    private Integer totalCount;
    /** 已生成数量。 */
    private Integer generatedCount;
    /** 已兑换数量。 */
    private Integer redeemedCount;
    /** 生效时间。 */
    private String validFrom;
    /** 失效时间。 */
    private String validTo;
    /** 状态。 */
    private String status;
    /** 风险等级。 */
    private String riskLevel;
    /** 创建人。 */
    private String createdBy;
    /** 审批人。 */
    private String approvedBy;
    /** 审批时间。 */
    private String approvedAt;
    /** 导出次数。 */
    private Integer exportCount;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
