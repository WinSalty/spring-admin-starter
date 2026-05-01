package com.winsalty.quickstart.credential.vo;

import lombok.Data;

/**
 * 凭证导入任务展示对象。
 * 展示导入统计、结果摘要、批次归属和任务状态。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialImportTaskVo {

    private String id;
    private String taskNo;
    private String batchId;
    private String categoryId;
    private String categoryName;
    private String delimiter;
    private Integer totalRows;
    private Integer validRows;
    private Integer duplicateRows;
    private Integer invalidRows;
    private String importHash;
    private String resultSummary;
    private String status;
    private String createdBy;
    private String createdAt;
    private String updatedAt;
}
