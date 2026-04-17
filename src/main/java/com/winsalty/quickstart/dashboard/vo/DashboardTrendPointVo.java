package com.winsalty.quickstart.dashboard.vo;

/**
 * 工作台趋势点响应对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public class DashboardTrendPointVo {

    private String date;
    private Long visits;
    private Long orders;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Long getVisits() {
        return visits;
    }

    public void setVisits(Long visits) {
        this.visits = visits;
    }

    public Long getOrders() {
        return orders;
    }

    public void setOrders(Long orders) {
        this.orders = orders;
    }
}
