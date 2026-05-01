package com.winsalty.quickstart.credential.dto;

import lombok.Data;

/**
 * 凭证导入任务分页请求。
 * 支持按分类、批次和状态查询导入任务。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialImportTaskListRequest {

    private Long categoryId;
    private Long batchId;
    private String status;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
