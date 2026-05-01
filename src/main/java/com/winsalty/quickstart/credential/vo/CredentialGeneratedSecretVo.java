package com.winsalty.quickstart.credential.vo;

import lombok.Data;

/**
 * 系统生成凭证明文展示对象。
 * 仅在创建批次的响应中一次性返回，用于管理员复制本次生成的 CDK。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialGeneratedSecretVo {

    /** 凭证明细编号。 */
    private String itemNo;
    /** 本次生成的凭证明文。 */
    private String secretText;
    /** 前端复制时展示的标签。 */
    private String copyLabel;
}
