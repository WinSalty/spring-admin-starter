package com.winsalty.quickstart.common.util;

import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 客户端 User-Agent 解析工具。
 * 在不引入额外依赖的前提下提取审计需要的浏览器、系统和设备字段。
 * 创建日期：2026-05-02
 * author：sunshengxian
 */
public final class ClientDeviceParser {

    public static final String DEVICE_TYPE_DESKTOP = "DESKTOP";
    public static final String DEVICE_TYPE_MOBILE = "MOBILE";
    public static final String DEVICE_TYPE_TABLET = "TABLET";
    public static final String DEVICE_BRAND_PC = "PC";
    public static final String DEVICE_BRAND_APPLE = "Apple";
    public static final String DEVICE_BRAND_UNKNOWN = "UNKNOWN";
    private static final String BROWSER_CHROME = "Google Chrome";
    private static final String BROWSER_EDGE = "Microsoft Edge";
    private static final String BROWSER_FIREFOX = "Mozilla Firefox";
    private static final String BROWSER_SAFARI = "Apple Safari";
    private static final String BROWSER_WECHAT = "WeChat";
    private static final String BROWSER_UNKNOWN = "UNKNOWN";
    private static final String OS_WINDOWS = "Windows";
    private static final String OS_MACOS = "macOS";
    private static final String OS_IOS = "iOS";
    private static final String OS_ANDROID = "Android";
    private static final String OS_LINUX = "Linux";
    private static final String OS_UNKNOWN = "UNKNOWN";
    private static final Pattern EDGE_PATTERN = Pattern.compile("(?:Edg|Edge|EdgiOS|EdgA)/([\\w.]+)");
    private static final Pattern CHROME_PATTERN = Pattern.compile("(?:Chrome|CriOS)/([\\w.]+)");
    private static final Pattern FIREFOX_PATTERN = Pattern.compile("(?:Firefox|FxiOS)/([\\w.]+)");
    private static final Pattern SAFARI_VERSION_PATTERN = Pattern.compile("Version/([\\w.]+).*Safari/");
    private static final Pattern WECHAT_PATTERN = Pattern.compile("MicroMessenger/([\\w.]+)");
    private static final Pattern WINDOWS_PATTERN = Pattern.compile("Windows NT ([\\d.]+)");
    private static final Pattern MACOS_PATTERN = Pattern.compile("Mac OS X ([\\d_]+)");
    private static final Pattern IOS_PATTERN = Pattern.compile("(?:iPhone|iPad|iPod).*OS ([\\d_]+)");
    private static final Pattern ANDROID_PATTERN = Pattern.compile("Android ([\\d.]+)");

    private ClientDeviceParser() {
    }

    /**
     * 解析原始 User-Agent 为标准审计字段。
     */
    public static ClientDeviceInfo parse(String userAgent) {
        ClientDeviceInfo info = new ClientDeviceInfo();
        String normalized = userAgent == null ? "" : userAgent.trim();
        info.setUserAgent(normalized);
        if (!StringUtils.hasText(normalized)) {
            info.setBrowser("");
            info.setBrowserVersion("");
            info.setOsName("");
            info.setOsVersion("");
            info.setDeviceType("");
            info.setDeviceBrand("");
            return info;
        }
        fillBrowser(info, normalized);
        fillOs(info, normalized);
        fillDevice(info, normalized);
        return info;
    }

    private static void fillBrowser(ClientDeviceInfo info, String userAgent) {
        BrowserMatch browser = matchBrowser(userAgent);
        info.setBrowser(browser.name);
        info.setBrowserVersion(browser.version);
    }

    private static BrowserMatch matchBrowser(String userAgent) {
        String edgeVersion = findFirst(EDGE_PATTERN, userAgent);
        if (StringUtils.hasText(edgeVersion)) {
            return new BrowserMatch(BROWSER_EDGE, edgeVersion);
        }
        String chromeVersion = findFirst(CHROME_PATTERN, userAgent);
        if (StringUtils.hasText(chromeVersion)) {
            return new BrowserMatch(BROWSER_CHROME, chromeVersion);
        }
        String firefoxVersion = findFirst(FIREFOX_PATTERN, userAgent);
        if (StringUtils.hasText(firefoxVersion)) {
            return new BrowserMatch(BROWSER_FIREFOX, firefoxVersion);
        }
        String safariVersion = findFirst(SAFARI_VERSION_PATTERN, userAgent);
        if (StringUtils.hasText(safariVersion)) {
            return new BrowserMatch(BROWSER_SAFARI, safariVersion);
        }
        String wechatVersion = findFirst(WECHAT_PATTERN, userAgent);
        if (StringUtils.hasText(wechatVersion)) {
            return new BrowserMatch(BROWSER_WECHAT, wechatVersion);
        }
        return new BrowserMatch(BROWSER_UNKNOWN, "");
    }

    private static void fillOs(ClientDeviceInfo info, String userAgent) {
        String windowsVersion = findFirst(WINDOWS_PATTERN, userAgent);
        if (StringUtils.hasText(windowsVersion)) {
            info.setOsName(OS_WINDOWS);
            info.setOsVersion(mapWindowsVersion(windowsVersion));
            return;
        }
        String iosVersion = findFirst(IOS_PATTERN, userAgent);
        if (StringUtils.hasText(iosVersion)) {
            info.setOsName(OS_IOS);
            info.setOsVersion(iosVersion.replace('_', '.'));
            return;
        }
        String androidVersion = findFirst(ANDROID_PATTERN, userAgent);
        if (StringUtils.hasText(androidVersion)) {
            info.setOsName(OS_ANDROID);
            info.setOsVersion(androidVersion);
            return;
        }
        String macVersion = findFirst(MACOS_PATTERN, userAgent);
        if (StringUtils.hasText(macVersion)) {
            info.setOsName(OS_MACOS);
            info.setOsVersion(macVersion.replace('_', '.'));
            return;
        }
        if (userAgent.contains("Linux")) {
            info.setOsName(OS_LINUX);
            info.setOsVersion("");
            return;
        }
        info.setOsName(OS_UNKNOWN);
        info.setOsVersion("");
    }

    private static void fillDevice(ClientDeviceInfo info, String userAgent) {
        if (containsAny(userAgent, "iPad", "Tablet")) {
            info.setDeviceType(DEVICE_TYPE_TABLET);
        } else if (containsAny(userAgent, "Mobile", "iPhone", "Android")) {
            info.setDeviceType(DEVICE_TYPE_MOBILE);
        } else {
            info.setDeviceType(DEVICE_TYPE_DESKTOP);
        }
        info.setDeviceBrand(resolveDeviceBrand(userAgent, info.getDeviceType()));
    }

    private static String resolveDeviceBrand(String userAgent, String deviceType) {
        if (containsAny(userAgent, "iPhone", "iPad", "Macintosh")) {
            return DEVICE_BRAND_APPLE;
        }
        if (DEVICE_TYPE_DESKTOP.equals(deviceType)) {
            return DEVICE_BRAND_PC;
        }
        if (containsAny(userAgent, "HUAWEI", "Huawei")) {
            return "Huawei";
        }
        if (containsAny(userAgent, "HONOR", "Honor")) {
            return "Honor";
        }
        if (containsAny(userAgent, "Xiaomi", "Mi ", "Redmi")) {
            return "Xiaomi";
        }
        if (containsAny(userAgent, "OPPO")) {
            return "OPPO";
        }
        if (containsAny(userAgent, "vivo")) {
            return "vivo";
        }
        if (containsAny(userAgent, "Samsung", "SM-")) {
            return "Samsung";
        }
        return DEVICE_BRAND_UNKNOWN;
    }

    private static String mapWindowsVersion(String version) {
        if ("10.0".equals(version)) {
            return "10/11";
        }
        if ("6.3".equals(version)) {
            return "8.1";
        }
        if ("6.2".equals(version)) {
            return "8";
        }
        if ("6.1".equals(version)) {
            return "7";
        }
        return version;
    }

    private static boolean containsAny(String value, String... candidates) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static String findFirst(Pattern pattern, String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static class BrowserMatch {
        private final String name;
        private final String version;

        private BrowserMatch(String name, String version) {
            this.name = name;
            this.version = version;
        }
    }
}
