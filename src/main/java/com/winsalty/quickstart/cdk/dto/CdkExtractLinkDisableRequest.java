package com.winsalty.quickstart.cdk.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * CDK 提取链接停用请求。
 * 用于管理员主动关闭已经分发的临时提取 URL。
 * 创建日期：2026-04-30
 * author：sunshengxian
 */
@Data
public class CdkExtractLinkDisableRequest {

    /** 停用原因。 */
    @NotBlank(message = "停用原因不能为空")
    @Size(max = 256, message = "停用原因不能超过 256 个字符")
    private String reason;
}
