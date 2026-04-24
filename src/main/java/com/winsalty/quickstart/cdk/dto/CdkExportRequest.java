package com.winsalty.quickstart.cdk.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * CDK 加密导出请求。
 * 管理员提供导出密码，后端仅返回加密 ZIP 包并记录文件指纹。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class CdkExportRequest {

    /** 导出加密密码。 */
    @NotBlank(message = "导出密码不能为空")
    @Size(min = 12, max = 128, message = "导出密码长度必须在12到128之间")
    private String exportPassword;
}
