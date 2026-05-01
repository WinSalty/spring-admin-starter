package com.winsalty.quickstart.credential.vo;

import lombok.Data;

/**
 * 凭证提取访问记录展示对象。
 * 用于提取链接详情中展示访问结果、IP、指纹和设备摘要。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialExtractAccessRecordVo {

    private String id;
    private String accessNo;
    private String linkId;
    private String batchId;
    private Integer itemCount;
    private Boolean success;
    private String failureReason;
    private String clientIp;
    private String userAgentHash;
    private String browserFingerprint;
    private String deviceSnapshot;
    private String traceId;
    private String createdAt;
}
