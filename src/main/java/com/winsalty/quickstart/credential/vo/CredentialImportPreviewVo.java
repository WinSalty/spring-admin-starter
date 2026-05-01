package com.winsalty.quickstart.credential.vo;

import lombok.Data;

import java.util.List;

/**
 * 凭证导入预览展示对象。
 * 返回解析统计、脱敏样例和错误行摘要，避免原始卡密进入数据库或日志。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialImportPreviewVo {

    private Integer totalRows;
    private Integer validRows;
    private Integer duplicateRows;
    private Integer invalidRows;
    private String importHash;
    private List<CredentialImportPreviewItemVo> previews;
    private List<String> duplicateMessages;
    private List<String> invalidMessages;
}
