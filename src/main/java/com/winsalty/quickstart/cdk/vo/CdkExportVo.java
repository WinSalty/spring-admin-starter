package com.winsalty.quickstart.cdk.vo;

import lombok.Data;

import java.util.List;

/**
 * CDK 一次性导出展示对象。
 * 仅在受控导出接口返回明文码，审计日志不记录该响应体。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class CdkExportVo {

    private String batchNo;
    private Integer count;
    private String fingerprint;
    private List<String> codes;
}
