package com.winsalty.quickstart.cdk.entity;

import lombok.Data;

/**
 * CDK 提取访问记录实体。
 * 保存公开提取 URL 每次访问的结果、设备指纹、IP 和链路追踪信息。
 * 创建日期：2026-04-30
 * author：sunshengxian
 */
@Data
public class CdkExtractAccessRecordEntity {

    /** 主键ID。 */
    private Long id;
    /** 访问编号。 */
    private String accessNo;
    /** 提取链接ID。 */
    private Long linkId;
    /** CDK ID。 */
    private Long codeId;
    /** 批次ID。 */
    private Long batchId;
    /** 访问结果。 */
    private String result;
    /** 失败码。 */
    private String failureCode;
    /** 失败原因。 */
    private String failureMessage;
    /** 客户端 IP。 */
    private String clientIp;
    /** UA 摘要。 */
    private String userAgentHash;
    /** 浏览器指纹摘要。 */
    private String browserFingerprint;
    /** 设备快照 JSON。 */
    private String deviceSnapshot;
    /** 来源页面。 */
    private String referer;
    /** 链路 ID。 */
    private String traceId;
    /** 创建时间。 */
    private String createdAt;
}
