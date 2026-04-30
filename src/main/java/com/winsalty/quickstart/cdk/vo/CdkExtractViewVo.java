package com.winsalty.quickstart.cdk.vo;

import lombok.Data;

/**
 * CDK 公开提取页展示对象。
 * 向匿名访问者返回可复制 CDK、权益文案、有效期和剩余访问次数。
 * 创建日期：2026-04-30
 * author：sunshengxian
 */
@Data
public class CdkExtractViewVo {

    private String cdk;
    private String batchName;
    private String benefitType;
    private String benefitText;
    private String validTo;
    private Integer remainingAccessCount;
    private String status;
}
