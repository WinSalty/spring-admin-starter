package com.winsalty.quickstart.common.util;

import javax.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 地址提取工具。
 * 支持反向代理场景下从 X-Forwarded-For 等请求头中提取真实 IP。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
public class IpUtils {

    private IpUtils() {
    }

    /**
     * 从请求中提取客户端真实 IP 地址。
     * 依次检查 X-Forwarded-For、X-Real-IP 请求头，均无则取 remoteAddr。
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty() && !"unknown".equalsIgnoreCase(xff)) {
            return xff.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
