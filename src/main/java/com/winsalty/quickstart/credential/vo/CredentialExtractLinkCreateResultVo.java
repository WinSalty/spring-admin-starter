package com.winsalty.quickstart.credential.vo;

import lombok.Data;

import java.util.List;

/**
 * 凭证提取链接生成结果。
 * 返回生成链接数、覆盖凭证数和可复制链接列表。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialExtractLinkCreateResultVo {

    private Integer linkCount;
    private Integer itemCount;
    private List<CredentialExtractLinkCopyVo> links;
}
