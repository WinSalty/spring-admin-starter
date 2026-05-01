package com.winsalty.quickstart.credential.vo;

import lombok.Data;

/**
 * 用户凭证兑换结果。
 * 返回兑换记录号、入账积分、账本流水和凭证状态。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialRedeemVo {

    private String recordNo;
    private String itemNo;
    private Long points;
    private String ledgerNo;
    private String status;
}
