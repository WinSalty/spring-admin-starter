package com.winsalty.quickstart.dashboard.vo;

import java.util.List;

/**
 * 工作台概览响应对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public class DashboardOverviewVo {

    private List<DashboardMetricVo> metrics;
    private List<DashboardTrendPointVo> trend;
    private List<DashboardCategoryVo> categories;
    private List<DashboardStatusVo> statusDistribution;

    public List<DashboardMetricVo> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<DashboardMetricVo> metrics) {
        this.metrics = metrics;
    }

    public List<DashboardTrendPointVo> getTrend() {
        return trend;
    }

    public void setTrend(List<DashboardTrendPointVo> trend) {
        this.trend = trend;
    }

    public List<DashboardCategoryVo> getCategories() {
        return categories;
    }

    public void setCategories(List<DashboardCategoryVo> categories) {
        this.categories = categories;
    }

    public List<DashboardStatusVo> getStatusDistribution() {
        return statusDistribution;
    }

    public void setStatusDistribution(List<DashboardStatusVo> statusDistribution) {
        this.statusDistribution = statusDistribution;
    }
}
