package com.winsalty.quickstart.points.constant;

/**
 * 积分模块常量。
 * 统一维护账务方向、状态、业务类型和分页默认值，避免业务代码散落魔法值。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
public final class PointsConstants {

    public static final String ACCOUNT_STATUS_ACTIVE = "active";
    public static final String ACCOUNT_STATUS_DISABLED = "disabled";
    public static final String DIRECTION_EARN = "earn";
    public static final String DIRECTION_SPEND = "spend";
    public static final String DIRECTION_FREEZE = "freeze";
    public static final String DIRECTION_UNFREEZE = "unfreeze";
    public static final String DIRECTION_REFUND = "refund";
    public static final String BIZ_TYPE_CDK_RECHARGE = "cdk_recharge";
    public static final String BIZ_TYPE_ONLINE_RECHARGE = "online_recharge";
    public static final String BIZ_TYPE_ADMIN_ADJUST = "admin_adjust";
    public static final String BIZ_TYPE_BENEFIT_EXCHANGE = "benefit_exchange";
    public static final String CHANNEL_CDK = "cdk";
    public static final String CHANNEL_ONLINE_PAY = "online_pay";
    public static final String OPERATOR_TYPE_USER = "user";
    public static final String OPERATOR_TYPE_ADMIN = "admin";
    public static final String OPERATOR_TYPE_SYSTEM = "system";
    public static final String ORDER_STATUS_CREATED = "created";
    public static final String ORDER_STATUS_PROCESSING = "processing";
    public static final String ORDER_STATUS_SUCCESS = "success";
    public static final String ORDER_STATUS_FAILED = "failed";
    public static final String FREEZE_STATUS_FROZEN = "frozen";
    public static final String FREEZE_STATUS_CONFIRMED = "confirmed";
    public static final String FREEZE_STATUS_CANCELLED = "cancelled";
    public static final String ADJUST_STATUS_PENDING = "pending";
    public static final String ADJUST_STATUS_APPROVED = "approved";
    public static final String ADJUST_STATUS_REJECTED = "rejected";
    public static final String HASH_GENESIS = "GENESIS";
    public static final int DEFAULT_PAGE_NO = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    private PointsConstants() {
    }
}
