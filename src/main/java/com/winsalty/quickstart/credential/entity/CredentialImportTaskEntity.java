package com.winsalty.quickstart.credential.entity;

import lombok.Data;

/**
 * 凭证导入任务实体。
 * 保存文本卡密导入的解析统计、摘要和结果，不保存原始明文。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialImportTaskEntity {

    private Long id;
    private String taskNo;
    private Long batchId;
    private Long categoryId;
    private String categoryName;
    private String delimiter;
    private Integer totalRows;
    private Integer validRows;
    private Integer duplicateRows;
    private Integer invalidRows;
    private String importHash;
    private String resultSummary;
    private String status;
    private Long createdBy;
    private String createdAt;
    private String updatedAt;
}
