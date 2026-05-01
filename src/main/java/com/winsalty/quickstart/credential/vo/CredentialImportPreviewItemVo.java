package com.winsalty.quickstart.credential.vo;

import lombok.Data;

/**
 * 凭证导入预览明细展示对象。
 * 仅展示行号、脱敏值和解析状态，不暴露完整卡密。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialImportPreviewItemVo {

    private Integer lineNo;
    private String secretMask;
    private String status;
    private String message;
}
