package com.winsalty.quickstart.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 客户端设备解析工具测试。
 * 覆盖微信 PC 内嵌 Chromium UA 和移动端 Safari 等常见审计来源。
 * 创建日期：2026-05-02
 * author：sunshengxian
 */
class ClientDeviceParserTest {

    @Test
    void parseWindowsWechatChromiumUserAgent() {
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 NetType/WIFI MicroMessenger/7.0.20.1781(0x6700143B) WindowsWechat(0x63090a13) UnifiedPCWindowsWechat(0xf254173b) XWEB/19027 Flue";

        ClientDeviceInfo info = ClientDeviceParser.parse(userAgent);

        assertEquals(userAgent, info.getUserAgent());
        assertEquals("Google Chrome", info.getBrowser());
        assertEquals("132.0.0.0", info.getBrowserVersion());
        assertEquals("Windows", info.getOsName());
        assertEquals("10/11", info.getOsVersion());
        assertEquals("DESKTOP", info.getDeviceType());
        assertEquals("PC", info.getDeviceBrand());
    }

    @Test
    void parseIphoneSafariUserAgent() {
        String userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1";

        ClientDeviceInfo info = ClientDeviceParser.parse(userAgent);

        assertEquals("Apple Safari", info.getBrowser());
        assertEquals("17.2", info.getBrowserVersion());
        assertEquals("iOS", info.getOsName());
        assertEquals("17.2", info.getOsVersion());
        assertEquals("MOBILE", info.getDeviceType());
        assertEquals("Apple", info.getDeviceBrand());
    }
}
