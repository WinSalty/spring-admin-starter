package com.salty.admin.log.service;

import com.salty.admin.log.entity.SysLogLogin;
import com.salty.admin.log.mapper.SysLogLoginMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class LoginLogService {

    private static final Logger log = LoggerFactory.getLogger(LoginLogService.class);

    private final SysLogLoginMapper loginMapper;

    public LoginLogService(SysLogLoginMapper loginMapper) {
        this.loginMapper = loginMapper;
    }

    @Async
    public void record(String username, String ip, String userAgent, boolean success, String message) {
        try {
            SysLogLogin loginLog = new SysLogLogin();
            loginLog.setUsername(username);
            loginLog.setLoginIp(ip);
            loginLog.setLoginLocation("local");
            loginLog.setBrowser(parseBrowser(userAgent));
            loginLog.setOs(parseOs(userAgent));
            loginLog.setStatus(success ? 1 : 0);
            loginLog.setMsg(message);
            loginLog.setLoginTime(LocalDateTime.now());
            loginMapper.insert(loginLog);
        } catch (Exception ex) {
            log.warn("Failed to record login log, username={}, ip={}, success={}", username, ip, success, ex);
        }
    }

    private String parseBrowser(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "Unknown";
        }
        if (userAgent.contains("Chrome")) {
            return "Chrome";
        }
        if (userAgent.contains("Firefox")) {
            return "Firefox";
        }
        if (userAgent.contains("Safari")) {
            return "Safari";
        }
        return "Unknown";
    }

    private String parseOs(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "Unknown";
        }
        if (userAgent.contains("Mac OS")) {
            return "macOS";
        }
        if (userAgent.contains("Windows")) {
            return "Windows";
        }
        if (userAgent.contains("Linux")) {
            return "Linux";
        }
        return "Unknown";
    }
}
