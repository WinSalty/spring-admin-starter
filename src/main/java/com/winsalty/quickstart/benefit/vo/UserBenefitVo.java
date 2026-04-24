package com.winsalty.quickstart.benefit.vo;

import lombok.Data;

/**
 * 用户权益展示对象。
 * 用于展示用户已获得的权限、服务包等权益。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class UserBenefitVo {

    private String id;
    private String userId;
    private String benefitType;
    private String benefitCode;
    private String benefitName;
    private String sourceType;
    private String sourceNo;
    private String status;
    private String effectiveAt;
    private String expireAt;
    private String configSnapshot;
    private String createdAt;
    private String updatedAt;
}
