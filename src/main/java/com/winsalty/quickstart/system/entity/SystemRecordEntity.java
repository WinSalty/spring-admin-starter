package com.winsalty.quickstart.system.entity;

import lombok.Data;

/**
 * 系统管理通用记录实体对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class SystemRecordEntity {

    /** 通用记录主键ID。 */
    private Long id;
    /** 记录编码。 */
    private String recordCode;
    /** 模块标识。 */
    private String moduleKey;
    /** 记录名称。 */
    private String name;
    /** 记录编码字段。 */
    private String code;
    /** 记录状态。 */
    private String status;
    /** 负责人。 */
    private String owner;
    /** 描述信息。 */
    private String description;
    /** 用户头像地址。 */
    private String avatarUrl;
    /** 部门名称。 */
    private String department;
    /** 部门ID。 */
    private Long departmentId;
    /** 角色名称列表。 */
    private String roleNames;
    /** 角色编码列表。 */
    private String roleCodes;
    /** 最近登录时间。 */
    private String lastLoginAt;
    /** 数据权限范围。 */
    private String dataScope;
    /** 用户数量。 */
    private Long userCount;
    /** 字典类型。 */
    private String dictType;
    /** 字典项数量。 */
    private Long itemCount;
    /** 缓存键。 */
    private String cacheKey;
    /** 日志类型。 */
    private String logType;
    /** 目标对象。 */
    private String target;
    /** IP 地址。 */
    private String ipAddress;
    /** 设备信息。 */
    private String deviceInfo;
    /** 请求信息。 */
    private String requestInfo;
    /** 响应信息。 */
    private String responseInfo;
    /** 操作结果。 */
    private String result;
    /** 耗时毫秒数。 */
    private Long durationMs;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
