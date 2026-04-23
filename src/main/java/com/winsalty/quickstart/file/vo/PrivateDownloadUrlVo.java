package com.winsalty.quickstart.file.vo;

import lombok.Data;

/**
 * 私有文件临时下载地址响应。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Data
public class PrivateDownloadUrlVo {

    /** 文件ID。 */
    private String fileId;
    /** 下载地址，本地存储时为后端代理下载地址。 */
    private String downloadUrl;
    /** 有效秒数。 */
    private Long expireSeconds;
    /** 下载方式：signed_url 或 proxy_stream。 */
    private String downloadMode;
}
