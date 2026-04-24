package com.winsalty.quickstart.cdk.vo;

import lombok.Data;

/**
 * CDK 一次性导出展示对象。
 * 仅返回加密 ZIP 包，不再直接返回明文码；审计日志不记录该响应体。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class CdkExportVo {

    private String batchNo;
    private Integer count;
    private String fingerprint;
    private String fileName;
    private String fileType;
    private String encryptionAlgorithm;
    private String encryptedPackageBase64;
}
