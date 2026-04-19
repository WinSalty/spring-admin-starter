package com.winsalty.quickstart.dashboard.vo;

import lombok.Data;

import java.util.List;

/**
 * 工作台概览响应对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class DashboardOverviewVo {

    private List<DashboardMetricVo> metrics;
    private List<DashboardTrendPointVo> trend;
    private List<DashboardCategoryVo> categories;
    private List<DashboardStatusVo> statusDistribution;
}
