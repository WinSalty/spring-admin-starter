package com.winsalty.quickstart.credential.entity;

import lombok.Data;

/**
 * 凭证操作审计实体。
 * 记录管理员对凭证、提取链接和导入任务的高敏操作。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialOperationAuditEntity {

    /** 主键ID。 */
    private Long id;
    /** 审计编号。 */
    private String auditNo;
    /** 操作人ID。 */
    private Long operatorId;
    /** 操作类型。 */
    private String operationType;
    /** 目标类型。 */
    private String targetType;
    /** 目标ID。 */
    private Long targetId;
    /** 操作前脱敏快照。 */
    private String beforeSnapshot;
    /** 操作后脱敏快照。 */
    private String afterSnapshot;
    /** 管理端IP。 */
    private String clientIp;
    /** 是否成功。 */
    private Integer success;
    /** 失败原因。 */
    private String failureReason;
    /** 创建时间。 */
    private String createdAt;
}
