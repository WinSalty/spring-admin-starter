package com.winsalty.quickstart.credential.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 凭证提取链接延期请求。
 * 用新的过期时间覆盖原过期时间，并写入操作审计。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialExtractLinkExtendRequest {

    /** 新过期时间，格式为 yyyy-MM-dd HH:mm:ss。 */
    @NotBlank(message = "过期时间不能为空")
    private String expireAt;
}
