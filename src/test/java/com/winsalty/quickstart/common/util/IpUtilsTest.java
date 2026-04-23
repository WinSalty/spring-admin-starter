package com.winsalty.quickstart.common.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 客户端 IP 解析测试。
 * 验证仅在可信代理场景下才信任转发头，避免直连请求伪造来源地址。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
class IpUtilsTest {

    @AfterEach
    void tearDown() {
        new IpUtils().setTrustedProxies("127.0.0.1/32,::1/128");
    }

    @Test
    void getClientIpUsesRemoteAddrWhenRequestDoesNotComeFromTrustedProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.8");

        assertEquals("203.0.113.10", IpUtils.getClientIp(request));
    }

    @Test
    void getClientIpUsesForwardedHeaderWhenRemoteAddrIsTrustedProxy() {
        IpUtils ipUtils = new IpUtils();
        ipUtils.setTrustedProxies("127.0.0.1/32,10.0.0.0/8");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.10.0.5");
        request.addHeader("X-Forwarded-For", "198.51.100.8, 10.10.0.5");

        assertEquals("198.51.100.8", IpUtils.getClientIp(request));
    }
}
