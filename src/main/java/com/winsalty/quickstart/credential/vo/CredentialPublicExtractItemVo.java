package com.winsalty.quickstart.credential.vo;

import lombok.Data;

/**
 * 公开提取凭证明细展示对象。
 * 返回单条可复制的凭证明文和前端复制标签。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialPublicExtractItemVo {

    private String itemNo;
    private String secretText;
    private String copyLabel;
}
