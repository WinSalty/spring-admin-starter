package com.winsalty.quickstart.dashboard.service.impl;

import com.winsalty.quickstart.dashboard.service.DashboardService;
import com.winsalty.quickstart.dashboard.vo.DashboardCategoryVo;
import com.winsalty.quickstart.dashboard.vo.DashboardMetricVo;
import com.winsalty.quickstart.dashboard.vo.DashboardOverviewVo;
import com.winsalty.quickstart.dashboard.vo.DashboardStatusVo;
import com.winsalty.quickstart.dashboard.vo.DashboardTrendPointVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作台服务实现。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    @Override
    public DashboardOverviewVo getOverview() {
        log.info("dashboard overview loaded");
        DashboardOverviewVo response = new DashboardOverviewVo();
        response.setMetrics(buildMetrics());
        response.setTrend(buildTrend());
        response.setCategories(buildCategories());
        response.setStatusDistribution(buildStatusDistribution());
        return response;
    }

    private List<DashboardMetricVo> buildMetrics() {
        List<DashboardMetricVo> metrics = new ArrayList<DashboardMetricVo>();
        metrics.add(buildMetric("visits", "今日访问", 12860L, "次", null, "较昨日", "+12.4%", "up"));
        metrics.add(buildMetric("orders", "有效订单", 864L, "单", null, "转化率", "8.2%", "stable"));
        metrics.add(buildMetric("revenue", "交易金额", 326800L, "元", null, "较上周", "+6.8%", "up"));
        metrics.add(buildMetric("alerts", "待处理告警", 18L, "条", null, "较昨日", "-9.1%", "down"));
        return metrics;
    }

    private DashboardMetricVo buildMetric(String key,
                                          String title,
                                          Long value,
                                          String suffix,
                                          Integer precision,
                                          String trendLabel,
                                          String trendValue,
                                          String trendType) {
        DashboardMetricVo metric = new DashboardMetricVo();
        metric.setKey(key);
        metric.setTitle(title);
        metric.setValue(value);
        metric.setSuffix(suffix);
        metric.setPrecision(precision);
        metric.setTrendLabel(trendLabel);
        metric.setTrendValue(trendValue);
        metric.setTrendType(trendType);
        return metric;
    }

    private List<DashboardTrendPointVo> buildTrend() {
        List<DashboardTrendPointVo> trend = new ArrayList<DashboardTrendPointVo>();
        trend.add(buildTrendPoint("04-10", 8600L, 520L));
        trend.add(buildTrendPoint("04-11", 9400L, 610L));
        trend.add(buildTrendPoint("04-12", 10200L, 680L));
        trend.add(buildTrendPoint("04-13", 9800L, 640L));
        trend.add(buildTrendPoint("04-14", 11600L, 720L));
        trend.add(buildTrendPoint("04-15", 12100L, 790L));
        trend.add(buildTrendPoint("04-16", 12860L, 864L));
        return trend;
    }

    private DashboardTrendPointVo buildTrendPoint(String date, Long visits, Long orders) {
        DashboardTrendPointVo point = new DashboardTrendPointVo();
        point.setDate(date);
        point.setVisits(visits);
        point.setOrders(orders);
        return point;
    }

    private List<DashboardCategoryVo> buildCategories() {
        List<DashboardCategoryVo> categories = new ArrayList<DashboardCategoryVo>();
        categories.add(buildCategory("查询管理", 246L));
        categories.add(buildCategory("数据统计", 198L));
        categories.add(buildCategory("权限目录", 126L));
        categories.add(buildCategory("系统配置", 92L));
        categories.add(buildCategory("消息中心", 78L));
        return categories;
    }

    private DashboardCategoryVo buildCategory(String name, Long value) {
        DashboardCategoryVo category = new DashboardCategoryVo();
        category.setName(name);
        category.setValue(value);
        return category;
    }

    private List<DashboardStatusVo> buildStatusDistribution() {
        List<DashboardStatusVo> statusDistribution = new ArrayList<DashboardStatusVo>();
        statusDistribution.add(buildStatus("运行中", 62L));
        statusDistribution.add(buildStatus("待处理", 18L));
        statusDistribution.add(buildStatus("已完成", 138L));
        statusDistribution.add(buildStatus("异常", 7L));
        return statusDistribution;
    }

    private DashboardStatusVo buildStatus(String name, Long value) {
        DashboardStatusVo status = new DashboardStatusVo();
        status.setName(name);
        status.setValue(value);
        return status;
    }
}
