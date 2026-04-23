package com.winsalty.quickstart.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户端 IP 地址提取工具。
 * 仅在请求来源属于可信代理时才解析 X-Forwarded-For / X-Real-IP，避免客户端直连伪造请求头。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Component
public class IpUtils {

    private static volatile List<IpRange> trustedProxyRanges = defaultTrustedProxyRanges();

    public IpUtils() {
    }

    /**
     * 从请求中提取客户端真实 IP 地址。
     * 只有 remoteAddr 命中可信代理名单时才信任转发头，否则直接返回 remoteAddr。
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String remoteAddr = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddr)) {
            return normalizeIp(remoteAddr);
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (hasText(xff)) {
            String[] forwardedIps = xff.split(",");
            for (String forwardedIp : forwardedIps) {
                String candidate = normalizeIp(forwardedIp);
                if (hasText(candidate) && !"unknown".equalsIgnoreCase(candidate)) {
                    return candidate;
                }
            }
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (hasText(xRealIp) && !"unknown".equalsIgnoreCase(xRealIp.trim())) {
            return normalizeIp(xRealIp);
        }
        return normalizeIp(remoteAddr);
    }

    /**
     * 配置可信代理列表，仅命中这些来源地址时才解析代理头。
     */
    @Value("${app.security.trusted-proxies:127.0.0.1/32,::1/128}")
    public void setTrustedProxies(String trustedProxies) {
        trustedProxyRanges = parseRanges(trustedProxies);
    }

    static boolean isTrustedProxy(String remoteAddr) {
        String normalizedRemoteAddr = normalizeIp(remoteAddr);
        if (!hasText(normalizedRemoteAddr)) {
            return false;
        }
        for (IpRange range : trustedProxyRanges) {
            if (range.contains(normalizedRemoteAddr)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeIp(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static List<IpRange> parseRanges(String trustedProxies) {
        List<IpRange> ranges = new ArrayList<>();
        if (!hasText(trustedProxies)) {
            return defaultTrustedProxyRanges();
        }
        String[] entries = trustedProxies.split(",");
        for (String entry : entries) {
            String candidate = normalizeIp(entry);
            if (!hasText(candidate)) {
                continue;
            }
            try {
                ranges.add(IpRange.of(candidate));
            } catch (IllegalArgumentException exception) {
                // 解析失败时忽略无效配置项，避免单个错误值导致服务不可用。
            }
        }
        return ranges.isEmpty() ? defaultTrustedProxyRanges() : ranges;
    }

    private static List<IpRange> defaultTrustedProxyRanges() {
        List<IpRange> ranges = new ArrayList<>();
        ranges.add(IpRange.of("127.0.0.1/32"));
        ranges.add(IpRange.of("::1/128"));
        return ranges;
    }

    /**
     * 简单 IP/CIDR 匹配器，支持精确 IP 和 CIDR 网段。
     */
    static final class IpRange {

        private final BigInteger network;
        private final BigInteger mask;
        private final int bitSize;

        private IpRange(BigInteger network, BigInteger mask, int bitSize) {
            this.network = network;
            this.mask = mask;
            this.bitSize = bitSize;
        }

        static IpRange of(String expression) {
            String[] parts = expression.split("/");
            try {
                InetAddress address = InetAddress.getByName(parts[0].trim());
                int bitSize = address.getAddress().length * 8;
                int prefixLength = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : bitSize;
                if (prefixLength < 0 || prefixLength > bitSize) {
                    throw new IllegalArgumentException("invalid prefix length");
                }
                BigInteger mask = prefixLength == 0
                        ? BigInteger.ZERO
                        : BigInteger.ONE.shiftLeft(bitSize).subtract(BigInteger.ONE)
                        .xor(BigInteger.ONE.shiftLeft(bitSize - prefixLength).subtract(BigInteger.ONE));
                BigInteger network = toBigInteger(address).and(mask);
                return new IpRange(network, mask, bitSize);
            } catch (UnknownHostException exception) {
                throw new IllegalArgumentException("invalid ip range", exception);
            }
        }

        boolean contains(String ip) {
            try {
                InetAddress address = InetAddress.getByName(ip);
                if (address.getAddress().length * 8 != bitSize) {
                    return false;
                }
                return toBigInteger(address).and(mask).equals(network);
            } catch (UnknownHostException exception) {
                return false;
            }
        }

        private static BigInteger toBigInteger(InetAddress address) {
            return new BigInteger(1, address.getAddress());
        }
    }
}
