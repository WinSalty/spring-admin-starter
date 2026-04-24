package com.winsalty.quickstart.benefit.constant;

/**
 * 权益兑换常量。
 * 统一维护权益类型、订单状态和分页默认值，避免业务代码散落魔法值。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
public final class BenefitConstants {

    public static final String BENEFIT_TYPE_PERMISSION = "permission";
    public static final String BENEFIT_TYPE_SERVICE_PACKAGE = "service_package";
    public static final String PRODUCT_STATUS_ACTIVE = "active";
    public static final String PRODUCT_STATUS_DISABLED = "disabled";
    public static final String ORDER_STATUS_PROCESSING = "processing";
    public static final String ORDER_STATUS_SUCCESS = "success";
    public static final String ORDER_STATUS_FAILED = "failed";
    public static final String USER_BENEFIT_STATUS_ACTIVE = "active";
    public static final String SOURCE_TYPE_POINT_EXCHANGE = "point_exchange";
    public static final int UNLIMITED_STOCK = -1;
    public static final int DEFAULT_PAGE_NO = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    private BenefitConstants() {
    }
}
