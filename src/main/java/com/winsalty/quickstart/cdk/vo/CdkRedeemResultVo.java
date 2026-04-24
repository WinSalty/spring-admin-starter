package com.winsalty.quickstart.cdk.vo;

import lombok.Data;

/**
 * CDK 兑换结果展示对象。
 * 只返回业务结果和权益摘要，不返回任何敏感内部字段。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class CdkRedeemResultVo {

    private String redeemNo;
    private String benefitType;
    private Long grantedPoints;
    private Long availablePoints;
    private Long frozenPoints;
    private String status;
}
