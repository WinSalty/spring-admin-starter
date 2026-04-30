package com.winsalty.quickstart.cdk.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

/**
 * CDK 提取访问记录分页请求。
 * 用于管理员按结果和浏览器指纹查询临时 URL 的访问审计流水。
 * 创建日期：2026-04-30
 * author：sunshengxian
 */
@Data
public class CdkExtractAccessRecordListRequest {

    /** 访问结果。 */
    @Size(max = 32, message = "访问结果长度不能超过 32 个字符")
    private String result;

    /** 浏览器指纹摘要。 */
    @Size(max = 128, message = "浏览器指纹长度不能超过 128 个字符")
    private String fingerprint;

    /** 页码。 */
    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNo = 1;

    /** 每页条数。 */
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Integer pageSize = 10;
}
