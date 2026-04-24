package com.winsalty.quickstart.cdk.entity;

import lombok.Data;

/**
 * CDK 导出审计实体。
 * 仅记录导出元数据和文件指纹，不记录明文 CDK 内容。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class CdkExportAuditEntity {

    /** 主键ID。 */
    private Long id;
    /** 批次ID。 */
    private Long batchId;
    /** 批次号。 */
    private String batchNo;
    /** 导出人。 */
    private String exportedBy;
    /** 导出数量。 */
    private Integer exportCount;
    /** 文件指纹。 */
    private String fileFingerprint;
    /** 创建时间。 */
    private String createdAt;
}
