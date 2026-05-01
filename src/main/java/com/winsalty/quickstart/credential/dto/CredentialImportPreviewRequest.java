package com.winsalty.quickstart.credential.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 文本卡密导入预览请求。
 * 后端按固定分隔符解析、脱敏预览并完成批内和全库去重检查。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialImportPreviewRequest {

    private Long categoryId;

    @NotBlank(message = "卡密文本不能为空")
    private String rawText;

    private String delimiter = "\\n";
    private Boolean trimBlank = true;
    private Boolean batchDeduplicate = true;
    private Boolean globalDeduplicate = true;
    private Boolean caseSensitive = true;
}
