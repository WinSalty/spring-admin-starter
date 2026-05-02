package com.winsalty.quickstart.common.util;

import lombok.Data;

/**
 * 客户端设备解析结果。
 * 承载审计日志和公开提取访问记录需要保存的浏览器、系统和设备维度。
 * 创建日期：2026-05-02
 * author：sunshengxian
 */
@Data
public class ClientDeviceInfo {

    /** 原始 User-Agent。 */
    private String userAgent;
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

    /**
     * 返回简短设备说明，兼容旧 device_info 展示字段。
     */
    public String toDeviceInfo() {
        StringBuilder builder = new StringBuilder();
        append(builder, browser);
        append(builder, browserVersion);
        append(builder, osName);
        append(builder, osVersion);
        append(builder, deviceType);
        append(builder, deviceBrand);
        return builder.toString();
    }

    private void append(StringBuilder builder, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(value.trim());
    }
}
