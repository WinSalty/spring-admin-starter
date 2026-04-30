package com.winsalty.quickstart.cdk.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * CDK 提取链接创建请求。
 * 用于管理员为单个 CDK 生成带访问次数和过期时间的临时提取 URL。
 * 创建日期：2026-04-30
 * author：sunshengxian
 */
@Data
public class CdkExtractLinkCreateRequest {

    /** 最大访问次数。 */
    @Min(value = 1, message = "访问次数不能小于 1")
    @Max(value = 100, message = "访问次数不能超过 100")
    private Integer maxAccessCount;

    /** 过期时间，格式 yyyy-MM-dd HH:mm:ss。 */
    @NotBlank(message = "过期时间不能为空")
    private String expireAt;

    /** 备注。 */
    @Size(max = 512, message = "备注不能超过 512 个字符")
    private String remark;
}
