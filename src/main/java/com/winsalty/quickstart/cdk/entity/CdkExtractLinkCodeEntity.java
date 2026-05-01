package com.winsalty.quickstart.cdk.entity;

import lombok.Data;

/**
 * CDK 提取链接码明细实体。
 * 表达一个临时提取链接可同时展示多个 CDK 的关联关系和排序。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CdkExtractLinkCodeEntity {

    /** 主键ID。 */
    private Long id;
    /** 提取链接ID。 */
    private Long linkId;
    /** CDK ID。 */
    private Long codeId;
    /** 批次ID。 */
    private Long batchId;
    /** 链接内排序。 */
    private Integer sortNo;
    /** 创建时间。 */
    private String createdAt;
}
