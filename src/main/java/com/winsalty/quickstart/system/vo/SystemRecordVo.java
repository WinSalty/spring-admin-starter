package com.winsalty.quickstart.system.vo;

import lombok.Data;

/**
 * 系统管理响应对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class SystemRecordVo {

    private String id;
    private String moduleKey;
    private String name;
    private String code;
    private String status;
    private String owner;
    private String description;
    private String avatarUrl;
    private String department;
    private String departmentId;
    private String roleNames;
    private String roleCodes;
    private String lastLoginAt;
    private String dataScope;
    private Long userCount;
    private String dictType;
    private Long itemCount;
    private String cacheKey;
    private String logType;
    private String target;
    private String ipAddress;
    private String deviceInfo;
    private String requestInfo;
    private String responseInfo;
    private String result;
    private Long durationMs;
    private String createdAt;
    private String updatedAt;
}
