package com.winsalty.quickstart.credential.dto;

import lombok.Data;

/**
 * 凭证提取访问记录分页请求。
 * 支持按访问结果和设备指纹查看某个提取链接的访问审计。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialExtractAccessRecordListRequest {

    /** 是否成功，1 成功，0 失败。 */
    private Integer success;
    /** 浏览器指纹。 */
    private String fingerprint;
    /** 页码。 */
    private Integer pageNo;
    /** 每页条数。 */
    private Integer pageSize;
}
