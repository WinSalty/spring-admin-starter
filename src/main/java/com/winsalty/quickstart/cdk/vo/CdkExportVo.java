package com.winsalty.quickstart.cdk.vo;

import lombok.Data;

/**
 * CDK 导出展示对象。
 * 返回纯文本内容，审计日志不记录该响应体。
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
    private String content;
}
