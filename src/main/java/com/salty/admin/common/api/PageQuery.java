package com.salty.admin.common.api;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class PageQuery {

    @Min(value = 1, message = "pageNo must be greater than 0")
    private long pageNo = 1;

    @Min(value = 1, message = "pageSize must be greater than 0")
    @Max(value = 200, message = "pageSize must not exceed 200")
    private long pageSize = 10;

    private String sortField;

    private String sortOrder;

    public long getPageNo() {
        return pageNo;
    }

    public void setPageNo(long pageNo) {
        this.pageNo = pageNo;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
}
