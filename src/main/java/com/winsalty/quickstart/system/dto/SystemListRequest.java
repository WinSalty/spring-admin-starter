package com.winsalty.quickstart.system.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

/**
 * 系统管理列表请求对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public class SystemListRequest {

    @Pattern(regexp = "users|roles|dicts|logs", message = "moduleKey 不合法")
    private String moduleKey;

    private String keyword;

    @Pattern(regexp = "active|disabled", message = "状态值不合法")
    private String status;

    @Pattern(regexp = "login|operation|api", message = "logType 不合法")
    private String logType;

    @Min(value = 1, message = "pageNo 不能小于 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize 不能小于 1")
    @Max(value = 100, message = "pageSize 不能大于 100")
    private Integer pageSize = 10;

    public String getModuleKey() {
        return moduleKey;
    }

    public void setModuleKey(String moduleKey) {
        this.moduleKey = moduleKey;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
