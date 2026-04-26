package com.winsalty.quickstart.cdk.constant;

/**
 * CDK 模块常量。
 * 统一维护批次状态、码状态、权益类型、Redis key 前缀和格式规则。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
public final class CdkConstants {

    public static final String BENEFIT_TYPE_POINTS = "points";
    public static final String BATCH_STATUS_ACTIVE = "active";
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
    public static final String ALERT_STATUS_OPEN = "open";
    public static final String SUBJECT_TYPE_USER = "user";
    public static final String SUBJECT_TYPE_CDK_BATCH = "cdk_batch";
    public static final String REDEEM_USER_LIMIT_PREFIX = "sa:cdk:limit:user:";
    public static final String REDEEM_IP_LIMIT_PREFIX = "sa:cdk:limit:ip:";
    public static final String REDEEM_FAIL_PREFIX = "sa:cdk:fail:user:";
    public static final String REDEEM_LOCK_PREFIX = "sa:cdk:lock:user:";
    public static final String CHECKSUM_SALT = "winsalty-cdk-checksum-v1";
    public static final int DEFAULT_PAGE_NO = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int RANDOM_BYTE_LENGTH = 6;
    public static final int CODE_GROUP_LENGTH = 4;
    public static final int CHECKSUM_LENGTH = 1;
    public static final int HEX_CHARS_PER_BYTE = 2;
    public static final int CHECKSUM_PART_COUNT = 1;
    public static final int RANDOM_HEX_LENGTH = RANDOM_BYTE_LENGTH * HEX_CHARS_PER_BYTE;
    public static final int RANDOM_CODE_GROUP_COUNT = RANDOM_HEX_LENGTH / CODE_GROUP_LENGTH;
    public static final int CODE_PART_COUNT = RANDOM_CODE_GROUP_COUNT + CHECKSUM_PART_COUNT;

    private CdkConstants() {
    }
}
