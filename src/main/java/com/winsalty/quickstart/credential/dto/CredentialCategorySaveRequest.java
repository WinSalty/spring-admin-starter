package com.winsalty.quickstart.credential.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 凭证分类保存请求。
 * 用于管理端创建或更新凭证分类配置。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialCategorySaveRequest {

    @NotBlank(message = "分类编码不能为空")
    @Size(max = 64, message = "分类编码不能超过 64 个字符")
    private String categoryCode;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称不能超过 100 个字符")
    private String categoryName;

    @NotBlank(message = "履约类型不能为空")
    private String fulfillmentType;

    @NotBlank(message = "生成模式不能为空")
    private String generationMode;

    private String payloadSchema;
    private String importConfig;
    private String extractPolicy;
    private String status;
}
