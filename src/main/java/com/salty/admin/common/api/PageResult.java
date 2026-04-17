package com.salty.admin.common.api;

import java.util.Collections;
import java.util.List;

public class PageResult<T> {

    private List<T> records;

    private long pageNo;

    private long pageSize;

    private long total;

    public PageResult() {
    }

    public PageResult(List<T> records, long pageNo, long pageSize, long total) {
        this.records = records;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.total = total;
    }

    public static <T> PageResult<T> empty(long pageNo, long pageSize) {
        return new PageResult<T>(Collections.<T>emptyList(), pageNo, pageSize, 0);
    }

    public static <T> PageResult<T> of(List<T> records, long pageNo, long pageSize, long total) {
        return new PageResult<T>(records, pageNo, pageSize, total);
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

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

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
