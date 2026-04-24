package com.winsalty.quickstart.benefit.entity;

import lombok.Data;

/**
 * 用户权益实体。
 * 表示用户通过积分兑换或其他来源获得的权限、服务包等权益。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class UserBenefitEntity {

    /** 主键ID。 */
    private Long id;
    /** 用户ID。 */
    private Long userId;
    /** 权益类型。 */
    private String benefitType;
    /** 权益编码。 */
    private String benefitCode;
    /** 权益名称。 */
    private String benefitName;
    /** 来源类型。 */
    private String sourceType;
    /** 来源单号。 */
    private String sourceNo;
    /** 状态。 */
    private String status;
    /** 生效时间。 */
    private String effectiveAt;
    /** 失效时间。 */
    private String expireAt;
    /** 配置快照。 */
    private String configSnapshot;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
