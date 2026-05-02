package com.winsalty.quickstart.credential.entity;

import lombok.Data;

/**
 * 凭证提取访问记录实体。
 * 记录公开提取链接的成功和失败访问，用于运营审计和风险排查。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialExtractAccessRecordEntity {

    /** 主键ID。 */
    private Long id;
    /** 访问流水号。 */
    private String accessNo;
    /** 链接ID。 */
    private Long linkId;
    /** 批次ID。 */
    private Long batchId;
    /** 本次返回凭证数量。 */
    private Integer itemCount;
    /** 是否成功。 */
    private Integer success;
    /** 失败原因。 */
    private String failureReason;
    /** 客户端 IP。 */
    private String clientIp;
    /** 原始 User-Agent。 */
    private String userAgent;
    /** UA 摘要。 */
    private String userAgentHash;
    /** 浏览器名称。 */
    private String browser;
    /** 浏览器版本。 */
    private String browserVersion;
    /** 操作系统名称。 */
    private String osName;
    /** 操作系统版本。 */
    private String osVersion;
    /** 设备类型。 */
    private String deviceType;
    /** 设备品牌。 */
    private String deviceBrand;
    /** 浏览器指纹。 */
    private String browserFingerprint;
    /** 设备摘要。 */
    private String deviceSnapshot;
    /** 链路追踪ID。 */
    private String traceId;
    /** 创建时间。 */
    private String createdAt;
}
