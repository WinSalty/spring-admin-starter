package com.winsalty.quickstart.cdk.constant;

/**
 * CDK 模块常量。
 * 统一维护批次状态、码状态、权益类型、Redis key 前缀和格式规则。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
public final class CdkConstants {

    /**
     * 积分权益类型，CDK 当前仅支持兑换积分。
     */
    public static final String BENEFIT_TYPE_POINTS = "points";

    /**
     * 批次可用状态，只有该状态下的批次允许兑换或重新启用码。
     */
    public static final String BATCH_STATUS_ACTIVE = "active";

    /**
     * 批次已作废状态，整批失效后未兑换 CDK 会同步失效。
     */
    public static final String BATCH_STATUS_VOIDED = "voided";

    /**
     * CDK 可兑换状态，兑换时必须从该状态原子更新为已兑换。
     */
    public static final String CODE_STATUS_ACTIVE = "active";

    /**
     * CDK 已兑换状态，该状态禁止再次启用或重复兑换。
     */
    public static final String CODE_STATUS_REDEEMED = "redeemed";

    /**
     * CDK 已失效状态，运营可手动设置或由整批作废同步设置。
     */
    public static final String CODE_STATUS_DISABLED = "disabled";

    /**
     * 兑换记录处理中状态，代表码状态已经开始进入兑换事务。
     */
    public static final String RECORD_STATUS_PROCESSING = "processing";

    /**
     * 兑换记录成功状态，代表权益已完成发放。
     */
    public static final String RECORD_STATUS_SUCCESS = "success";

    /**
     * 兑换记录失败状态，保留给补偿或异常落库场景使用。
     */
    public static final String RECORD_STATUS_FAILED = "failed";

    /**
     * 普通风险等级，用于低风险批次或告警。
     */
    public static final String RISK_LEVEL_NORMAL = "normal";

    /**
     * 高风险等级，用于连续失败锁定等需要运营关注的事件。
     */
    public static final String RISK_LEVEL_HIGH = "high";

    /**
     * 严重风险等级，预留给高价值批次或异常资金事件。
     */
    public static final String RISK_LEVEL_CRITICAL = "critical";

    /**
     * CDK 兑换锁定告警类型。
     */
    public static final String ALERT_TYPE_REDEEM_LOCKED = "cdk_redeem_locked";

    /**
     * 告警待处理状态。
     */
    public static final String ALERT_STATUS_OPEN = "open";

    /**
     * 风险主体类型：用户。
     */
    public static final String SUBJECT_TYPE_USER = "user";

    /**
     * 风险主体类型：CDK 批次。
     */
    public static final String SUBJECT_TYPE_CDK_BATCH = "cdk_batch";

    /**
     * Redis 单用户兑换限流 key 前缀，后缀使用摘要避免直接暴露用户标识。
     */
    public static final String REDEEM_USER_LIMIT_PREFIX = "sa:cdk:limit:user:";

    /**
     * Redis 单 IP 兑换限流 key 前缀，后缀使用摘要避免直接暴露 IP。
     */
    public static final String REDEEM_IP_LIMIT_PREFIX = "sa:cdk:limit:ip:";

    /**
     * Redis 单用户连续失败计数 key 前缀。
     */
    public static final String REDEEM_FAIL_PREFIX = "sa:cdk:fail:user:";

    /**
     * Redis 单用户兑换锁定 key 前缀。
     */
    public static final String REDEEM_LOCK_PREFIX = "sa:cdk:lock:user:";

    /**
     * CDK 校验位固定盐，仅用于格式校验，不承担核心安全职责。
     */
    public static final String CHECKSUM_SALT = "winsalty-cdk-checksum-v1";

    /**
     * 默认页码。
     */
    public static final int DEFAULT_PAGE_NO = 1;

    /**
     * 默认分页大小。
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大分页大小，避免管理端一次拉取过多 CDK 明文。
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * CDK 随机字节数，8 字节等价于 64 bit 随机空间。
     */
    public static final int RANDOM_BYTE_LENGTH = 8;

    /**
     * CDK 展示分组长度，每组 4 个十六进制字符。
     */
    public static final int CODE_GROUP_LENGTH = 4;

    /**
     * CDK 校验位长度，当前使用 1 个十六进制字符。
     */
    public static final int CHECKSUM_LENGTH = 1;

    /**
     * 单字节转十六进制后的字符数。
     */
    public static final int HEX_CHARS_PER_BYTE = 2;

    /**
     * 校验位占用的分组数量。
     */
    public static final int CHECKSUM_PART_COUNT = 1;

    /**
     * 随机部分十六进制字符总长度。
     */
    public static final int RANDOM_HEX_LENGTH = RANDOM_BYTE_LENGTH * HEX_CHARS_PER_BYTE;

    /**
     * 随机部分展示分组数量。
     */
    public static final int RANDOM_CODE_GROUP_COUNT = RANDOM_HEX_LENGTH / CODE_GROUP_LENGTH;

    /**
     * 完整 CDK 分组数量，包含随机分组和末尾校验位。
     */
    public static final int CODE_PART_COUNT = RANDOM_CODE_GROUP_COUNT + CHECKSUM_PART_COUNT;

    private CdkConstants() {
    }
}
