package com.winsalty.quickstart.credential.vo;

import lombok.Data;

/**
 * 凭证提取链接复制结果。
 * 管理端复制 URL 时返回完整公开访问地址，并由服务层写操作审计。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialExtractLinkCopyVo {

    /** 链接ID。 */
    private String id;
    /** 链接编号。 */
    private String linkNo;
    /** 完整公开 URL。 */
    private String url;
}
