package com.winsalty.quickstart.benefit.dto;

import lombok.Data;

/**
 * 权益发放结果。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class BenefitGrantResult {

    private String benefitType;
    private Long grantedPoints;
    private Long availablePoints;
    private Long frozenPoints;
    private String snapshot;
}
