package com.winsalty.quickstart.cdk.constant;

/**
 * CDK 模块常量。
 * 统一维护批次状态、码状态、权益类型、Redis key 前缀和格式规则。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
public final class CdkConstants {

    public static final String BENEFIT_TYPE_POINTS = "points";
    public static final String BATCH_STATUS_DRAFT = "draft";
    public static final String BATCH_STATUS_PENDING_APPROVAL = "pending_approval";
    public static final String BATCH_STATUS_ACTIVE = "active";
    public static final String BATCH_STATUS_PAUSED = "paused";
    public static final String BATCH_STATUS_VOIDED = "voided";
    public static final String CODE_STATUS_ACTIVE = "active";
    public static final String CODE_STATUS_REDEEMED = "redeemed";
    public static final String CODE_STATUS_DISABLED = "disabled";
    public static final String RECORD_STATUS_PROCESSING = "processing";
    public static final String RECORD_STATUS_SUCCESS = "success";
    public static final String RECORD_STATUS_FAILED = "failed";
    public static final String RISK_LEVEL_NORMAL = "normal";
    public static final String RISK_LEVEL_HIGH = "high";
    public static final String RISK_LEVEL_CRITICAL = "critical";
    public static final String ALERT_TYPE_REDEEM_LOCKED = "cdk_redeem_locked";
    public static final String ALERT_TYPE_BATCH_DOUBLE_REVIEW = "cdk_batch_double_review";
    public static final String ALERT_STATUS_OPEN = "open";
    public static final String SUBJECT_TYPE_USER = "user";
    public static final String SUBJECT_TYPE_CDK_BATCH = "cdk_batch";
    public static final String CODE_PREFIX = "WSA";
    public static final String EXPORT_CACHE_PREFIX = "sa:cdk:export:";
    public static final String REDEEM_USER_LIMIT_PREFIX = "sa:cdk:limit:user:";
    public static final String REDEEM_IP_LIMIT_PREFIX = "sa:cdk:limit:ip:";
    public static final String REDEEM_FAIL_PREFIX = "sa:cdk:fail:user:";
    public static final String REDEEM_LOCK_PREFIX = "sa:cdk:lock:user:";
    public static final String CHECKSUM_SALT = "winsalty-cdk-checksum-v1";
    public static final int DEFAULT_PAGE_NO = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int RANDOM_BYTE_LENGTH = 16;
    public static final int CODE_GROUP_LENGTH = 4;
    public static final int MIN_CODE_PARTS = 7;
    public static final int CHECKSUM_LENGTH = 1;
    public static final int YEAR_MONTH_LENGTH = 6;

    private CdkConstants() {
    }
}
