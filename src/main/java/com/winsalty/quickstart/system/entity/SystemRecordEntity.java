package com.winsalty.quickstart.system.entity;

/**
 * 系统管理通用记录实体对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
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
    /** 操作结果。 */
    private String result;
    /** 耗时毫秒数。 */
    private Long durationMs;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecordCode() {
        return recordCode;
    }

    public void setRecordCode(String recordCode) {
        this.recordCode = recordCode;
    }

    public String getModuleKey() {
        return moduleKey;
    }

    public void setModuleKey(String moduleKey) {
        this.moduleKey = moduleKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getRoleNames() {
        return roleNames;
    }

    public void setRoleNames(String roleNames) {
        this.roleNames = roleNames;
    }

    public String getRoleCodes() {
        return roleCodes;
    }

    public void setRoleCodes(String roleCodes) {
        this.roleCodes = roleCodes;
    }

    public String getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(String lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getDataScope() {
        return dataScope;
    }

    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }

    public Long getUserCount() {
        return userCount;
    }

    public void setUserCount(Long userCount) {
        this.userCount = userCount;
    }

    public String getDictType() {
        return dictType;
    }

    public void setDictType(String dictType) {
        this.dictType = dictType;
    }

    public Long getItemCount() {
        return itemCount;
    }

    public void setItemCount(Long itemCount) {
        this.itemCount = itemCount;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
