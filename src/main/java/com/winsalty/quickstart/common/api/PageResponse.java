package com.winsalty.quickstart.common.api;

import java.util.List;

/**
 * 统一分页响应对象。
 * 字段名与前端 PageResult 保持一致，避免每个列表接口重复做适配。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public class PageResponse<T> {

    private List<T> records;
    private long pageNo;
    private long pageSize;
    private long total;

    public PageResponse() {
    }

    /**
     * @param records 当前页数据
     * @param pageNo 当前页码，从 1 开始
     * @param pageSize 每页条数
     * @param total 符合筛选条件的总记录数
     */
    public PageResponse(List<T> records, long pageNo, long pageSize, long total) {
        this.records = records;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.total = total;
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
