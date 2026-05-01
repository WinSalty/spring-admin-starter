package com.winsalty.quickstart.credential.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 凭证提取链接停用请求。
 * 管理员停用链接时可填写原因，原因会写入备注和操作审计。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialExtractLinkDisableRequest {

    /** 停用原因。 */
    @Size(max = 255, message = "停用原因不能超过255个字符")
    private String reason;
}
