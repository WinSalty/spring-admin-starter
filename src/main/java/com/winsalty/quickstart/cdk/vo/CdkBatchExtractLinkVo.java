package com.winsalty.quickstart.cdk.vo;

import lombok.Data;

import java.util.List;

/**
 * CDK 批次提取链接生成结果。
 * 用于管理端一次性展示批次内全部可提取 CDK 的临时 URL。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CdkBatchExtractLinkVo {

    private String batchId;
    private String batchNo;
    private String batchName;
    private Integer generatedCount;
    private Integer skippedCount;
    private List<CdkExtractLinkVo> links;
}
