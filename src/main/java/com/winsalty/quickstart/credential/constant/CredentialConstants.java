package com.winsalty.quickstart.credential.constant;

/**
 * 凭证中心常量。
 * 统一维护分类、履约类型、状态、风控和分页默认值，避免业务代码散落魔法值。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
public final class CredentialConstants {

    public static final String CATEGORY_POINTS_CDK = "POINTS_CDK";
    public static final String CATEGORY_TEXT_CARD_SECRET = "TEXT_CARD_SECRET";
    public static final String FULFILLMENT_TYPE_POINTS_REDEEM = "POINTS_REDEEM";
    public static final String FULFILLMENT_TYPE_TEXT_SECRET = "TEXT_SECRET";
    public static final String GENERATION_MODE_SYSTEM_GENERATED = "SYSTEM_GENERATED";
    public static final String GENERATION_MODE_TEXT_IMPORTED = "TEXT_IMPORTED";
    public static final String SOURCE_TYPE_GENERATED = "generated";
    public static final String SOURCE_TYPE_IMPORTED = "imported";
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_DISABLED = "disabled";
    public static final String STATUS_FINISHED = "finished";
    public static final String ITEM_STATUS_LINKED = "linked";
    public static final String ITEM_STATUS_EXTRACTED = "extracted";
    public static final String ITEM_STATUS_REDEEMED = "redeemed";
    public static final String LINK_STATUS_EXPIRED = "expired";
    public static final String LINK_STATUS_EXHAUSTED = "exhausted";
    public static final String RECORD_STATUS_SUCCESS = "success";
    public static final String RECORD_STATUS_FAILED = "failed";
    public static final String ALERT_STATUS_OPEN = "open";
    public static final String ALERT_TYPE_REDEEM_LOCKED = "credential_redeem_locked";
    public static final String SUBJECT_TYPE_USER = "user";
    public static final String BENEFIT_TYPE_POINTS = "points";
    public static final String BIZ_TYPE_CREDENTIAL_REDEEM = "credential_redeem";
    public static final String CHANNEL_CREDENTIAL = "credential";
    public static final int DEFAULT_PAGE_NO = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    private CredentialConstants() {
    }
}
