package com.winsalty.quickstart.log.dto;

import lombok.Data;

/**
 * 操作日志请求对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class OperationLogRequest {

    private String logType;
    private String owner;
    private String name;
    private String code;
    private String description;
    private String target;
    private String ipAddress;
    private String deviceInfo;
    private String requestInfo;
    private String responseInfo;
    private String result;
    private Long durationMs;
}
