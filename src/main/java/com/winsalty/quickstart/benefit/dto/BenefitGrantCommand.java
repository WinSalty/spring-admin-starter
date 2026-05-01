package com.winsalty.quickstart.benefit.dto;

import lombok.Data;

/**
 * 权益发放命令。
 * 凭证兑换和后续积分兑换统一通过该命令进入权益发放适配层。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class BenefitGrantCommand {

    /** 用户ID。 */
    private Long userId;
    /** 权益类型。 */
    private String benefitType;
    /** 权益配置 JSON。 */
    private String benefitConfig;
    /** 业务单号。 */
    private String bizNo;
    /** 幂等键。 */
    private String idempotencyKey;
    /** 操作人类型。 */
    private String operatorType;
    /** 操作人ID。 */
    private String operatorId;
    /** 备注。 */
    private String remark;
}
